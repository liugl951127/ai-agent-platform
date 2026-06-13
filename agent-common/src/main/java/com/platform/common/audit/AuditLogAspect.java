package com.platform.common.audit;

import cn.hutool.json.JSONUtil;
import com.platform.common.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 审计日志切面
 * <p>
 * 拦截 &#64;AuditLog 注解,异步写日志(JSON 行 / 文件 / DB 任选)
 * <p>
 * 隐私保护: 对 password / token / apiKey 等敏感字段,会被脱敏为 "******"
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    /** 敏感字段名(脱敏) */
    private static final java.util.Set<String> SENSITIVE = java.util.Set.of(
            "password", "passwd", "pwd",
            "token", "accessToken", "refreshToken", "jwt",
            "apiKey", "api_key", "secret", "privateKey"
    );

    @Around("@annotation(com.platform.common.audit.AuditLog)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        AuditLog anno = method.getAnnotation(AuditLog.class);

        AuditLogRecord rec = new AuditLogRecord();
        rec.setModule(anno.module());
        rec.setAction(anno.action());
        rec.setResourceType(anno.resourceType());
        rec.setResourceId(parseSpel(anno.resourceId(), method, pjp.getArgs()));
        rec.setTenantId(TenantContext.getTenantId());
        rec.setCreateTime(LocalDateTime.now());

        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra != null) {
            HttpServletRequest req = sra.getRequest();
            rec.setMethod(req.getMethod());
            rec.setUrl(req.getRequestURI());
            rec.setIp(getClientIp(req));
            rec.setUa(req.getHeader("User-Agent"));
            rec.setUserId(parseUserId(req));
        }

        Object result = null;
        Throwable error = null;
        try {
            if (anno.saveRequest()) {
                rec.setRequestArgs(maskArgs(pjp.getArgs()));
            }
            result = pjp.proceed();
            rec.setStatus(1);
            if (anno.saveResponse() && result != null) {
                rec.setResponseData(maskJson(result));
            }
            return result;
        } catch (Throwable t) {
            error = t;
            rec.setStatus(0);
            rec.setErrorMsg(t.getMessage() == null ? t.getClass().getName() : t.getMessage());
            throw t;
        } finally {
            rec.setCostMs((int) (System.currentTimeMillis() - start));
            writeLog(rec);
            if (error != null) log.warn("审计: 操作失败 {}/{}", anno.module(), anno.action());
        }
    }

    @Async
    public void writeLog(AuditLogRecord rec) {
        // 默认: 写 JSON 行(后续可替换为 Kafka / DB)
        log.info("AUDIT {}", JSONUtil.toJsonStr(rec));
    }

    // ----------------- helpers -----------------

    private String parseSpel(String spel, Method method, Object[] args) {
        if (spel == null || spel.isBlank() || !spel.contains("#")) return spel;
        EvaluationContext ctx = new StandardEvaluationContext();
        String[] names = NAME_DISCOVERER.getParameterNames(method);
        if (names != null) {
            for (int i = 0; i < names.length; i++) ctx.setVariable(names[i], args[i]);
        }
        try {
            Object v = PARSER.parseExpression(spel).getValue(ctx);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String maskArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        try {
            String json = JSONUtil.toJsonStr(args);
            return maskJsonString(json);
        } catch (Exception e) {
            return "[unserializable]";
        }
    }

    private String maskJson(Object obj) {
        try { return maskJsonString(JSONUtil.toJsonStr(obj)); }
        catch (Exception e) { return "[unserializable]"; }
    }

    /** 简单的 JSON 字符串脱敏: 找 "key":"value" 中 key 是否敏感 */
    private String maskJsonString(String json) {
        String out = json;
        for (String k : SENSITIVE) {
            // 匹配 "key":"xxx" 或 "key":123
            out = out.replaceAll("(\"" + k + "\"\\s*:\\s*)\"[^\"]*\"", "$1\"******\"");
        }
        return out;
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) ip = req.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }

    private Long parseUserId(HttpServletRequest req) {
        String h = req.getHeader("X-User-Id");
        if (h == null || h.isBlank()) return null;
        try { return Long.parseLong(h); } catch (NumberFormatException e) { return null; }
    }
}
