package com.wh.jobsbackend.application.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis mapper scanning configuration.
 */
@Configuration
@MapperScan("com.wh.jobsbackend.application.mapper")
public class DataMapperConfig {
}
