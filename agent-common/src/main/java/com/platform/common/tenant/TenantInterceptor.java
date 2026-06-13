package com.platform.common.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * 多租户拦截器
 * <p>
 * 解析请求头 X-Tenant-Id,塞到 TenantContext。
 * 网关层已解析过 X-User-Id,这里兼容两种来源(直接 / 网关透传)。
 * <p>
 * 配合 Spring MVC 注册使用(WebMvcConfigurer.addInterceptors),
 * 由于 common 模块不引入 spring-boot-starter-web 之外的拦截器注册,
 * 各服务在自己的 WebMvcConfig 中 import 此拦截器。
 */
@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    public static final String HEADER_TENANT_ID   = "X-Tenant-Id";
    public static final String HEADER_TENANT_CODE = "X-Tenant-Code";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        String tid = req.getHeader(HEADER_TENANT_ID);
        String tcode = req.getHeader(HEADER_TENANT_CODE);
        if (tid != null && !tid.isBlank()) {
            try {
                TenantContext.setTenantId(Long.parseLong(tid));
            } catch (NumberFormatException e) {
                log.warn("非法 X-Tenant-Id: {}", tid);
            }
        }
        if (tcode != null && !tcode.isBlank()) {
            TenantContext.setTenantCode(tcode);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }
}
