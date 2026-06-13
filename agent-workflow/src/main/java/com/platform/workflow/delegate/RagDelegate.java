package com.platform.workflow.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Slf4j
@Component
public class RagDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution ex) {
        String input = String.valueOf(ex.getVariable("input"));
        try {
            RestTemplate rt = new RestTemplate();
            Map<?,?> resp = rt.postForObject(
                "http://agent-knowledge/knowledge/search",
                Map.of("q", input, "topK", 3), Map.class);
            ex.setVariable("ragContext", resp);
        } catch (Exception e) {
            log.warn("RAG 调用失败: {}", e.getMessage());
        }
    }
}
