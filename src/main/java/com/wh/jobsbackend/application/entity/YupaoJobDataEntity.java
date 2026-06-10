package com.wh.jobsbackend.application.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hub_yupao_data")
public class YupaoJobDataEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;

    @TableField("job_id")
    private String jobId;
    @TableField("job_title")
    private String jobTitle;
    @TableField("job_link")
    private String jobLink;
    private String salary;
    private String location;
    private String experience;
    private String degree;
    @TableField("company_name")
    private String companyName;
    @TableField("hr_name")
    private String hrName;
    @TableField("delivery_status")
    private String deliveryStatus;
    @TableField("publish_time")
    private String publishTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
