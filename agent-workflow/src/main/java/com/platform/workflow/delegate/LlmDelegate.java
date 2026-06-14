package com.platform.workflow.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

/**
 * Flowable 节点 — 通过 HTTP 调 agent-llm (避免跨模块 Java 类强耦合)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution ex) {
        Long modelId = Long.valueOf(ex.getVariable("modelId").toString());
        String input = String.valueOf(ex.getVariable("input"));
        try {
            RestTemplate rt = new RestTemplate();
            Map<?,?> resp = rt.postForObject(
                "http://agent-llm/llm/chat?modelId=" + modelId,
                List.of(Map.of("role","user","content", input)),
                Map.class);
            String reply = String.valueOf(((Map<?,?>)resp.get("data")));
            ex.setVariable("reply", reply);
            log.info("LLM reply: {}", reply);
        } catch (Exception e) {
            log.error("LLM 调用失败, 设置 reply=error", e);
            ex.setVariable("reply", "ERROR: " + e.getMessage());
        }
    }
}
