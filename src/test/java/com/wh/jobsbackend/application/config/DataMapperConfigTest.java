package com.wh.jobsbackend.application.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.type.AnnotationMetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataMapperConfigTest {

    @Test
    void mapperScanShouldTargetMigratedMapperPackage() {
        AnnotationMetadata metadata = AnnotationMetadata.introspect(DataMapperConfig.class);
        String[] basePackages = (String[]) metadata
                .getAnnotationAttributes("org.mybatis.spring.annotation.MapperScan")
                .get("value");

        assertEquals(1, basePackages.length);
        assertEquals("com.wh.jobsbackend.application.mapper", basePackages[0]);
    }
}
