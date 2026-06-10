package com.wh.jobsbackend.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wh.jobsbackend.application.entity.UserJobTaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserJobTaskMapper extends BaseMapper<UserJobTaskEntity> {
}
