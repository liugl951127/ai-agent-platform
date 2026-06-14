package com.platform.tools.sql;

import com.platform.tools.api.Tool;
import com.platform.tools.api.ToolDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * SQL 查询工具 — 让 Agent 查 MySQL (限 SELECT, 带白名单)
 * <p>
 * 安全:
 *   - 只接受 SELECT (拒绝 INSERT/UPDATE/DELETE/DDL)
 *   - 强制 LIMIT (默认 100, 最大 1000)
 *   - 多语句检查 (拒绝 ';' 后还有内容)
 *   - 表名白名单 (application.yml 配置 sql.allowed-tables)
 */
@Component
@ToolDefinition(
    name = "sql_query",
    description = "MySQL 只读查询 (限 SELECT, 强制 LIMIT, 表名白名单). 用法: sql_query({sql:'SELECT id,name FROM user WHERE age>18', limit:50})",
    parameters = "{\"type\":\"object\",\"properties\":{" +
        "\"sql\":{\"type\":\"string\",\"description\":\"SELECT 语句\"}," +
        "\"limit\":{\"type\":\"integer\",\"description\":\"强制 LIMIT, 默认 100, 最大 1000\"}" +
        "},\"required\":[\"sql\"]}",
    category = "database"
)
public class SqlQueryTool implements Tool {

    private final DataSource dataSource;
    private final List<String> allowedTables;
    private final boolean enabled;

    /**
     * 构造函数 — Spring 会自动注入 DataSource (没配 DataSource 时会报 "no bean of type DataSource")
     * 为了让 agent-tools 在没配 DataSource 时也能启动, 用 ObjectProvider 软引用
     */
    public SqlQueryTool(
        org.springframework.beans.factory.ObjectProvider<DataSource> dataSourceProvider,
        @Value("${sql.allowed-tables:user,order,product}") String allowed,
        @Value("${sql.tool-enabled:false}") boolean enabled
    ) {
        this.dataSource = dataSourceProvider.getIfAvailable();
        this.allowedTables = Arrays.asList(allowed.split(","));
        this.enabled = enabled;
    }

    @Override
    public String name() { return "sql_query"; }

    @Override
    public Object execute(Map<String, Object> args) {
        if (!enabled) throw new IllegalStateException("sql_query 工具未启用 (sql.tool-enabled=false)");
        if (dataSource == null) throw new IllegalStateException("DataSource 未配置, sql_query 不可用");
        String sql = String.valueOf(args.get("sql")).trim();
        validate(sql);
        int limit = Math.min(((Number) args.getOrDefault("limit", 100)).intValue(), 1000);
        // 强制加 LIMIT
        if (!sql.toLowerCase().matches(".*\\blimit\\b.*")) {
            sql = sql + " LIMIT " + limit;
        }
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            int col = md.getColumnCount();
            List<String> headers = new ArrayList<>(col);
            for (int i = 1; i <= col; i++) headers.add(md.getColumnLabel(i));
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= col; i++) row.put(headers.get(i - 1), rs.getObject(i));
                rows.add(row);
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("headers", headers);
            out.put("rows", rows);
            out.put("count", rows.size());
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败: " + e.getMessage(), e);
        }
    }

    private void validate(String sql) {
        String s = sql.toLowerCase().trim();
        if (!s.startsWith("select") && !s.startsWith("with")) {
            throw new IllegalArgumentException("只允许 SELECT / WITH 语句");
        }
        if (s.contains(";") && s.indexOf(";") < s.length() - 1) {
            throw new IllegalArgumentException("拒绝多语句");
        }
        // 检查危险关键字
        for (String kw : List.of("insert", "update", "delete", "drop", "alter", "truncate", "grant", "revoke", "create")) {
            if (s.matches(".*\\b" + kw + "\\b.*")) {
                throw new IllegalArgumentException("包含禁止关键字: " + kw);
            }
        }
        // 表名白名单
        Set<String> tables = extractTables(s);
        for (String t : tables) {
            if (!allowedTables.contains(t)) {
                throw new IllegalArgumentException("表 '" + t + "' 不在白名单: " + allowedTables);
            }
        }
    }

    /** 简单 FROM/JOIN 提取表名, 不完美但够用 */
    private Set<String> extractTables(String sql) {
        Set<String> out = new HashSet<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "(?:from|join)\\s+`?([a-zA-Z_][a-zA-Z0-9_]*)`?",
            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(sql);
        while (m.find()) out.add(m.group(1).toLowerCase());
        return out;
    }
}
