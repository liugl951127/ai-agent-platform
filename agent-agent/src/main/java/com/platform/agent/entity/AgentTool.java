package com.platform.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_tool")
public class AgentTool extends BaseEntity {
    private String name;
    private String code;
    private String description;
    private String paramSchema;
    private String handler;
}
