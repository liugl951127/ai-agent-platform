package com.platform.common.gray;

import com.platform.common.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 灰度切面
 * <p>
 * 规则存储在内存 Map 中(简化),生产建议放到 Nacos 配置中心 / DB + 定时刷新。
 * <p>
 * 资源 → 灰度规则 的简易表达:
 *   resource    strategy      matchValue       含义
 *   /agent/chat TENANT_ID     "1,2"            租户 1 和 2 命中
 *   /agent/chat USER_ID       "100,200"        用户 100 和 200 命中
 *   /agent/chat IP            "192.168.1.1"    IP 命中
 *   /agent/chat RATIO         "30"             30% 用户命中(按 uid 哈希)
 */
@Slf4j
@Aspect
@Component
public class GrayReleaseAspect {

    /** 资源 → 规则 (简化版;生产应注入 GrayRuleService 查 DB / Nacos) */
    private static final java.util.Map<String, GrayRule> RULES = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        // 演示规则
        RULES.put("/agent/chat", new GrayRule("TENANT_ID", "2"));  // demo 租户走灰度
        RULES.put("/llm/chat",   new GrayRule("RATIO",     "20")); // 20% 流量
    }

    @Around("@annotation(com.platform.common.gray.GrayRelease)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        GrayRelease anno = sig.getMethod().getAnnotation(GrayRelease.class);
        String resource = anno.resource().isBlank()
                ? sig.getDeclaringTypeName() + "." + sig.getName()
                : anno.resource();

        try {
            if (match(resource)) {
                GrayContext.mark();
                log.debug("灰度命中: resource={}", resource);
            } else {
                GrayContext.unmark();
            }
            return pjp.proceed();
        } finally {
            GrayContext.clear();
        }
    }

    private boolean match(String resource) {
        GrayRule rule = RULES.get(resource);
        if (rule == null) return false;

        switch (rule.strategy) {
            case "TENANT_ID": {
                Long tid = TenantContext.getTenantId();
                return tid != null && rule.values.contains(String.valueOf(tid));
            }
            case "USER_ID": {
                Long uid = getUserIdFromHeader();
                return uid != null && rule.values.contains(String.valueOf(uid));
            }
            case "IP": {
                String ip = getClientIp();
                return ip != null && rule.values.contains(ip);
            }
            case "RATIO": {
                Long uid = getUserIdFromHeader();
                if (uid == null) return false;
                int ratio = Integer.parseInt(rule.values.iterator().next());
                return Math.abs(uid.intValue() % 100) < ratio;
            }
            default:
                return false;
        }
    }

    private Long getUserIdFromHeader() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra == null) return null;
        String h = sra.getRequest().getHeader("X-User-Id");
        if (h == null) return null;
        try { return Long.parseLong(h); } catch (NumberFormatException e) { return null; }
    }

    private String getClientIp() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra == null) return null;
        HttpServletRequest req = sra.getRequest();
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return ip;
    }

    /** 灰度规则内存模型 */
    public static class GrayRule {
        public final String strategy;
        public final Set<String> values;
        public GrayRule(String s, String csv) {
            this.strategy = s;
            this.values = new HashSet<>(Arrays.asList(csv.split(",")));
        }
    }
}
