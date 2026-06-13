package com.platform.workflow.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ToolDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution ex) {
        String tool = String.valueOf(ex.getVariable("tool"));
        log.info("调用工具: {}", tool);
        ex.setVariable("toolResult", "OK");
    }
}
