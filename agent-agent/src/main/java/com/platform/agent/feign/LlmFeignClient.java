package com.platform.agent.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

/**
 * 跨服务调用 agent-llm 的 Feign 客户端
 * <p>
 * 完全用 Map 作为参数 / 返回,避免跨服务依赖 entity 类
 * <p>
 * 即使 agent-llm 没启动,本服务的 /agent/chat 也能起 — 调用时会报错,但服务能起来
 */
@FeignClient(name = "agent-llm", path = "/llm")
public interface LlmFeignClient {
    @PostMapping("/chat")
    Map<String, Object> chat(@RequestParam("modelId") Long modelId,
                              @RequestBody List<Map<String, Object>> messages);
}
