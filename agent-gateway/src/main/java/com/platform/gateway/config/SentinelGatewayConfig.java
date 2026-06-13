package com.platform.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.*;

/**
 * Sentinel 网关限流配置
 * <p>
 * 默认规则:
 *   - /agent/**     QPS <= 20
 *   - /llm/**       QPS <= 10
 *   - /workflow/**  QPS <= 5
 *   - /chat/**      QPS <= 20
 *   - /knowledge/** QPS <= 15
 * 生产建议把规则推 Nacos 配置中心,Sentinel 自动刷新。
 */
@Configuration
public class SentinelGatewayConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler(
            ObjectProvider<List<ViewResolver>> viewResolversProvider) {
        return new SentinelGatewayBlockExceptionHandler(viewResolversProvider.getIfAvailable(Collections::emptyList));
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    @Bean
    public BlockRequestHandler blockRequestHandler() {
        return (exchange, ex) -> ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(
                    "{\"code\":429,\"message\":\"网关限流:请求过于频繁,请稍后再试\"}"));
    }

    @PostConstruct
    public void initRules() {
        List<GatewayFlowRule> rules = List.of(
            ruleOf("/agent/**",     20),
            ruleOf("/llm/**",       10),
            ruleOf("/workflow/**",   5),
            ruleOf("/chat/**",      20),
            ruleOf("/knowledge/**", 15)
        );
        loadRulesCompat(rules);
    }

    private GatewayFlowRule ruleOf(String pattern, int qps) {
        GatewayFlowRule r = new GatewayFlowRule(pattern);
        r.setResourceMode(RuleConstant.RESOURCE_MODE_ROUTE_ID);
        r.setGrade(RuleConstant.FLOW_GRADE_QPS);
        r.setCount(qps);
        return r;
    }

    @SuppressWarnings("unchecked")
    private void loadRulesCompat(List<GatewayFlowRule> rules) {
        try {
            Class.forName("com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayRules")
                .getMethod("loadRules", List.class)
                .invoke(null, rules);
        } catch (Exception e) {
            try {
                Class.forName("com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager")
                    .getMethod("loadRules", List.class)
                    .invoke(null, rules);
            } catch (Exception ignored) { /* 规则由 dashboard 加载 */ }
        }
    }
}
