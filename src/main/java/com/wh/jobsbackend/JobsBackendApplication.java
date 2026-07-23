package com.wh.jobsbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication(scanBasePackages = "com.wh.jobsbackend")
@EnableScheduling
@EnableAsync
public class JobsBackendApplication {
    private static final Logger log = LoggerFactory.getLogger(JobsBackendApplication.class);
    private static final String CODEX_BUILD_MARKER = "boss-cdp-security-id-20260719-v2";

    @Autowired(required = false)
    private BuildProperties buildProperties;

    public static void main(String[] args) {
        System.out.println("[BOOT-BREADCRUMB] build=" + CODEX_BUILD_MARKER);
        SpringApplication.run(JobsBackendApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String version = (buildProperties != null) ? buildProperties.getVersion() : "vb.260723.1522";
        log.info("==================================================");
        log.info(" JobSeeker Backend Started Successfully!");
        log.info(" Current Backend Version: {}", version);
        log.info("==================================================");
    }

}
