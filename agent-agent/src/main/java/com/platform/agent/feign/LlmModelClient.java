package com.platform.agent.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

/**
 * 跨服务调用 agent-llm 的 Feign 客户端
 * (返回 Map 避免跨服务依赖 entity 类)
 */
@FeignClient(name = "agent-llm", path = "/llm")
public interface LlmModelClient {
    @GetMapping("/list")
    Map<String, Object> list();

    @GetMapping("/detail/{id}")
    Map<String, Object> detail(@PathVariable("id") Long id);
}
