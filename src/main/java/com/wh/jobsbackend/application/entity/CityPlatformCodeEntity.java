package com.wh.jobsbackend.application.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hub_city_platform_code")
public class CityPlatformCodeEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long cityId;
    private String platform;
    private String platformCityCode;
    private String platformCityName;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
