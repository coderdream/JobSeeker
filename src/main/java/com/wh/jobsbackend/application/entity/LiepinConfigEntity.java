package com.wh.jobsbackend.application.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hub_liepin_config")
public class LiepinConfigEntity {
    @TableId(type = IdType.AUTO)
    /** 主键ID */
    private Long id;

    private Long userId;

    /** 搜索关键词 */
    private String keywords;

    /** 城市（名称或代码） */
    private String city;

    /** 薪资代码或范围 */
    private String salaryCode;

    private String compTag;
    private String pubTime;
    private String workYearCode;
    private String eduLevel;
    private String industry;
    private String jobKind;
    private String compScale;
    private String compStage;
    private String compKind;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
