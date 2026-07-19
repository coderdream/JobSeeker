package com.wh.jobsbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.wh.jobsbackend")
@EnableScheduling
@EnableAsync
public class JobsBackendApplication {
    private static final String CODEX_BUILD_MARKER = "boss-cdp-security-id-20260719-v2";

    public static void main(String[] args) {
        System.out.println("[BOOT-BREADCRUMB] build=" + CODEX_BUILD_MARKER);
        SpringApplication.run(JobsBackendApplication.class, args);
    }

}
