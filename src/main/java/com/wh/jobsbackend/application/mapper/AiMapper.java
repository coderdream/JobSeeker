package com.wh.jobsbackend.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.jobsbackend.application.entity.AiEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI配置Mapper接口
 */
@Mapper
public interface AiMapper extends BaseMapper<AiEntity> {
}
