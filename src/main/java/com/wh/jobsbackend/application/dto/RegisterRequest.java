package com.wh.jobsbackend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 32, message = "用户名长度必须为 4-32 位")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,
        @NotBlank(message = "昵称不能为空")
        @Size(min = 2, max = 32, message = "昵称长度必须为 2-32 位")
        String nickname,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须为 8-64 位")
        String password,
        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {
}
