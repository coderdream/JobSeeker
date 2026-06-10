package com.wh.jobsbackend.application.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hub_yupao_config")
public class YupaoConfigEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String keywords;
    private String cityCode;
    private String salary;
    private String jobType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
