package com.wh.jobsbackend.worker.utils;

import lombok.Getter;

@Getter
public enum Platform {
    ZHILIAN("智联招聘"),
    BOSS("Boss直聘"),
    LIEPIN("猎聘"),
    JOB51("前程无忧"),
    YUPAO("鱼泡直聘"),
    LAGOU("拉勾网"),
    UNKNOWN("未知平台");

    private final String platformName;

    Platform(String platformName) {
        this.platformName = platformName;
    }
}
