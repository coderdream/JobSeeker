package com.wh.jobsbackend.application.service;

import com.wh.jobsbackend.application.dto.LoginRequest;
import com.wh.jobsbackend.application.dto.LoginResponse;
import com.wh.jobsbackend.application.dto.RegisterRequest;
import com.wh.jobsbackend.application.dto.RegisterResponse;
import com.wh.jobsbackend.application.entity.UserEntity;
import com.wh.jobsbackend.application.mapper.UserMapper;
import com.wh.jobsbackend.application.security.AppUserPrincipal;
import com.wh.jobsbackend.application.security.JwtTokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return new LoginResponse(principal.getId(), principal.getUsername(), jwtTokenService.generateToken(principal));
    }

    public RegisterResponse register(RegisterRequest request) {
        validateRegisterRequest(request);
        ensureUsernameAvailable(request.username());

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setNickname(request.nickname());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        return new RegisterResponse(true, "注册成功，请登录", user.getId(), user.getUsername(), user.getNickname());
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        if (!request.password().matches(PASSWORD_PATTERN)) {
            throw new IllegalArgumentException("密码必须为 8-64 位，且同时包含字母和数字");
        }
    }

    private void ensureUsernameAvailable(String username) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, username).last("LIMIT 1");
        if (userMapper.selectOne(wrapper) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }
}
