package com.wh.jobsbackend.application.dto;

public record LoginResponse(
        Long id,
        String username,
        String token
) {
}
