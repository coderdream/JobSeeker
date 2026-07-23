package com.wh.jobsbackend.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        Map<String, String> result = new HashMap<>();
        if (buildProperties != null) {
            result.put("backendVersion", buildProperties.getVersion());
        } else {
            // Fallback during IDE run or when build-info is not generated
            result.put("backendVersion", "vb.unknown");
        }
        return result;
    }
}
