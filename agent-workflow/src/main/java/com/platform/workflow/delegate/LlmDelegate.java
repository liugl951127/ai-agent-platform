package com.platform.workflow.delegate;

import com.platform.llm.service.LlmRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmDelegate implements JavaDelegate {
    private final LlmRouter llmRouter;

    @Override
    public void execute(DelegateExecution ex) {
        Long modelId = Long.valueOf(ex.getVariable("modelId").toString());
        String input = String.valueOf(ex.getVariable("input"));
        String reply = llmRouter.chatById(modelId, List.of(Map.of("role","user","content", input)));
        ex.setVariable("reply", reply);
        log.info("LLM reply: {}", reply);
    }
}
