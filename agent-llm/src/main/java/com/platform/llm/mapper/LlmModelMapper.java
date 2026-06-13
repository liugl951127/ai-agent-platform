package com.platform.llm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.llm.entity.LlmModel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmModelMapper extends BaseMapper<LlmModel> {}
