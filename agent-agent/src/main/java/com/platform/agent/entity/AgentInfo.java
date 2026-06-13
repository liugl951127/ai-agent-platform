package com.platform.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_info")
public class AgentInfo extends BaseEntity {
    private String name;
    private String avatar;
    private String description;
    private String systemPrompt;
    private Long modelId;
    private String toolIds;
    private String workflowKey;
    private Long knowledgeId;
    private Long userId;
}
