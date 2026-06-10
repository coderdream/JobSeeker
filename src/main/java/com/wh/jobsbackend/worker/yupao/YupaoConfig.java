package com.wh.jobsbackend.worker.yupao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class YupaoConfig {
    private List<String> keywords = new ArrayList<>();
    private String cityCode = "all";
    private String salary = "";
    private String jobType = "";
}
