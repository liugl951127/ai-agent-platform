package com.platform.agent.controller;

import com.platform.agent.entity.AgentInfo;
import com.platform.agent.mapper.AgentInfoMapper;
import com.platform.agent.service.ReactExecutor;
import com.platform.common.core.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {
    private final AgentInfoMapper mapper;
    private final ReactExecutor react;

    @GetMapping("/list")
    public R<List<AgentInfo>> list() { return R.ok(mapper.selectList(null)); }

    @PostMapping("/save")
    public R<?> save(@RequestBody AgentInfo a) { return R.ok(mapper.insert(a)); }

    @PostMapping("/chat")
    public R<String> chat(@RequestParam Long agentId, @RequestParam String input) {
        return R.ok(react.run(agentId, input));
    }
}
