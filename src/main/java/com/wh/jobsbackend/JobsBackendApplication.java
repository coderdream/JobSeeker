package com.wh.jobsbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.wh.jobsbackend")
@EnableScheduling
@EnableAsync
public class JobsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobsBackendApplication.class, args);
    }

}
