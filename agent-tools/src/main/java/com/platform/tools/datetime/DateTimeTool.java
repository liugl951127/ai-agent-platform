package com.platform.tools.datetime;

import com.platform.tools.api.Tool;
import com.platform.tools.api.ToolDefinition;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TimeZone;

/**
 * 日期时间工具
 * <p>
 * 支持: 获取当前时间 / 时间加减 / 时区转换 / 格式化 / 解析
 */
@Component
@ToolDefinition(
    name = "datetime",
    description = "日期时间工具. 操作: now/curr_time, add (加时长), diff (差值), format, parse, zone (时区转换). 例: datetime({op:'now'}), datetime({op:'add', base:'2026-01-01', amount:7, unit:'DAY'}), datetime({op:'zone', time:'2026-06-14 10:00:00', fromZone:'UTC', toZone:'Asia/Shanghai'})",
    parameters = "{\"type\":\"object\",\"properties\":{" +
        "\"op\":{\"type\":\"string\",\"enum\":[\"now\",\"add\",\"diff\",\"format\",\"parse\",\"zone\"],\"description\":\"操作类型\"}," +
        "\"time\":{\"type\":\"string\",\"description\":\"时间字符串 (parse/format/zone 时必填)\"}," +
        "\"base\":{\"type\":\"string\",\"description\":\"基准时间字符串 (add/diff 时用), 默认 now\"}," +
        "\"amount\":{\"type\":\"number\",\"description\":\"数量 (add/diff 时用, 正数加 负数减)\"}," +
        "\"unit\":{\"type\":\"string\",\"enum\":[\"SECOND\",\"MINUTE\",\"HOUR\",\"DAY\",\"WEEK\",\"MONTH\",\"YEAR\"],\"description\":\"单位\"}," +
        "\"pattern\":{\"type\":\"string\",\"description\":\"格式 pattern, 默认 yyyy-MM-dd HH:mm:ss\"}," +
        "\"zone\":{\"type\":\"string\",\"description\":\"时区, 默认 Asia/Shanghai\"}," +
        "\"fromZone\":{\"type\":\"string\"},\"toZone\":{\"type\":\"string\"}" +
        "},\"required\":[\"op\"]}",
    category = "system"
)
public class DateTimeTool implements Tool {

    private static final DateTimeFormatter DEFAULT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String name() { return "datetime"; }

    @Override
    public Object execute(Map<String, Object> args) {
        String op = String.valueOf(args.get("op"));
        String zone = strOr(args, "zone", "Asia/Shanghai");
        ZoneId zid = ZoneId.of(zone);

        return switch (op) {
            case "now" -> {
                String pat = strOr(args, "pattern", "yyyy-MM-dd HH:mm:ss");
                yield LocalDateTime.now(zid).format(DateTimeFormatter.ofPattern(pat));
            }
            case "add" -> {
                String base = strOr(args, "base", null);
                LocalDateTime b = base == null ? LocalDateTime.now(zid) : LocalDateTime.parse(base, DEFAULT);
                long amount = ((Number) args.getOrDefault("amount", 0)).longValue();
                String unit = String.valueOf(args.getOrDefault("unit", "DAY"));
                yield b.plus(amount, toChronoUnit(unit)).format(DEFAULT);
            }
            case "diff" -> {
                String base = strOr(args, "base", null);
                String other = String.valueOf(args.get("time"));
                LocalDateTime b = base == null ? LocalDateTime.now(zid) : LocalDateTime.parse(base, DEFAULT);
                LocalDateTime o = LocalDateTime.parse(other, DEFAULT);
                String unit = String.valueOf(args.getOrDefault("unit", "DAY"));
                long v = java.time.temporal.ChronoUnit.SECONDS.between(b, o) / toChronoUnit(unit).getDuration().getSeconds();
                yield Map.of("amount", v, "unit", unit);
            }
            case "format" -> {
                String time = String.valueOf(args.get("time"));
                String pat = strOr(args, "pattern", "yyyy-MM-dd HH:mm:ss");
                LocalDateTime t = LocalDateTime.parse(time, DEFAULT);
                yield t.format(DateTimeFormatter.ofPattern(pat));
            }
            case "parse" -> {
                String time = String.valueOf(args.get("time"));
                String pat = strOr(args, "pattern", "yyyy-MM-dd HH:mm:ss");
                yield LocalDateTime.parse(time, DateTimeFormatter.ofPattern(pat))
                        .atZone(zid).toInstant().toEpochMilli();
            }
            case "zone" -> {
                String time = String.valueOf(args.get("time"));
                String fromZ = strOr(args, "fromZone", "UTC");
                String toZ = strOr(args, "toZone", "Asia/Shanghai");
                LocalDateTime t = LocalDateTime.parse(time, DEFAULT);
                ZonedDateTime src = t.atZone(ZoneId.of(fromZ));
                ZonedDateTime dst = src.withZoneSameInstant(ZoneId.of(toZ));
                yield dst.format(DEFAULT);
            }
            default -> throw new IllegalArgumentException("不支持的 op: " + op);
        };
    }

    private static java.time.temporal.ChronoUnit toChronoUnit(String u) {
        return switch (u.toUpperCase()) {
            case "SECOND" -> java.time.temporal.ChronoUnit.SECONDS;
            case "MINUTE" -> java.time.temporal.ChronoUnit.MINUTES;
            case "HOUR" -> java.time.temporal.ChronoUnit.HOURS;
            case "DAY" -> java.time.temporal.ChronoUnit.DAYS;
            case "WEEK" -> java.time.temporal.ChronoUnit.WEEKS;
            case "MONTH" -> java.time.temporal.ChronoUnit.MONTHS;
            case "YEAR" -> java.time.temporal.ChronoUnit.YEARS;
            default -> throw new IllegalArgumentException("不支持的单位: " + u);
        };
    }

    private static String strOr(Map<String, Object> args, String k, String def) {
        Object v = args.get(k);
        return v == null ? def : String.valueOf(v);
    }
}
