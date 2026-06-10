package com.wh.jobsbackend.application.dto;

public record RegisterResponse(
        boolean success,
        String message,
        Long id,
        String username,
        String nickname
) {
}
