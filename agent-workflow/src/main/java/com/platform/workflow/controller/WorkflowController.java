package com.platform.workflow.controller;

import com.platform.common.core.R;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {
    private final RuntimeService runtime;

    @PostMapping("/start/agentChat")
    public R<String> start(@RequestBody Map<String,Object> vars) {
        ProcessInstance pi = runtime.startProcessInstanceByKey("agentChat", vars);
        return R.ok(pi.getId());
    }
}
