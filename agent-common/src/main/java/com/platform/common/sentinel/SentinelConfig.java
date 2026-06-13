package com.platform.common.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.platform.common.core.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 限流配置
 * <p>
 * 各微服务只需在 application.yml 配 sentinel.transport.port 和规则(代码 / Nacos),
 * 本类提供:
 * 1) BlockException 统一异常处理,返回标准 R.fail(429)
 * 2) 预留 WebMvc 扩展点(后续可加 QPS 拦截器)
 */
@Slf4j
@Configuration
@ConditionalOnClass(BlockException.class)
public class SentinelConfig implements WebMvcConfigurer {

    /** 限流时返回 */
    @ExceptionHandler(BlockException.class)
    public R<?> handleBlock(BlockException e, HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(429);
        log.warn("限流命中: rule={}, path={}", e.getRule(), req.getRequestURI());
        return R.fail(429, "请求过于频繁,请稍后再试");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 预留:可加 Sentinel Web 拦截器做 URI 维度统计
    }
}
