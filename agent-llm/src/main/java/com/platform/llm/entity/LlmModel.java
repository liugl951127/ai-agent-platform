package com.platform.llm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("llm_model")
public class LlmModel extends BaseEntity {
    private String name;
    private String provider;
    private String modelName;
    private String apiBase;
    private String apiKey;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer status;
}
