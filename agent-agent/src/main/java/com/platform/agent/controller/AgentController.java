package com.platform.agent.controller;

import com.platform.agent.entity.AgentInfo;
import com.platform.agent.mapper.AgentInfoMapper;
import com.platform.agent.service.AgentCreationService;
import com.platform.agent.service.ReactExecutor;
import com.platform.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "智能体管理", description = "CRUD + ReAct 推理 + 分布式事务创建")
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {
    private final AgentInfoMapper mapper;
    private final ReactExecutor react;
    private final AgentCreationService creationService;

    @Operation(summary = "列出智能体")
    @GetMapping("/list")
    public R<List<AgentInfo>> list() { return R.ok(mapper.selectList(null)); }

    @Operation(summary = "保存智能体(单服务事务)")
    @PostMapping("/save")
    public R<?> save(@RequestBody AgentInfo a) { return R.ok(mapper.insert(a)); }

    @Operation(summary = "智能体对话(ReAct 推理,带 Sentinel 限流)")
    @PostMapping("/chat")
    public R<String> chat(@RequestParam Long agentId, @RequestParam String input) {
        return R.ok(react.run(agentId, input));
    }

    @Operation(summary = "跨服务创建智能体(Seata 分布式事务)")
    @PostMapping("/create")
    public R<Long> createWithTx(@RequestBody AgentInfo agent) {
        return R.ok(creationService.createAgentWithConfig(agent));
    }
}
