package com.wh.jobsbackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.jobsbackend.application.dto.RegisterRequest;
import com.wh.jobsbackend.application.dto.RegisterResponse;
import com.wh.jobsbackend.application.entity.UserEntity;
import com.wh.jobsbackend.application.mapper.UserMapper;
import com.wh.jobsbackend.application.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceRegisterTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void registerShouldCreateActiveUser() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(12L);
            return 1;
        });
        AuthService authService = new AuthService(authenticationManager, jwtTokenService, userMapper, passwordEncoder);

        RegisterResponse response = authService.register(new RegisterRequest("new_user", "新用户", "abc12345", "abc12345"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insert(captor.capture());
        UserEntity savedUser = captor.getValue();
        assertEquals(12L, response.id());
        assertEquals("new_user", response.username());
        assertEquals("新用户", response.nickname());
        assertEquals("USER", savedUser.getRole());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertNotEquals("abc12345", savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches("abc12345", savedUser.getPasswordHash()));
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        UserEntity existing = new UserEntity();
        existing.setUsername("taken_user");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        AuthService authService = new AuthService(authenticationManager, jwtTokenService, userMapper, passwordEncoder);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> authService.register(new RegisterRequest("taken_user", "昵称", "abc12345", "abc12345")));

        assertEquals("用户名已存在", error.getMessage());
    }

    @Test
    void registerShouldRejectMismatchedPasswords() {
        AuthService authService = new AuthService(authenticationManager, jwtTokenService, userMapper, passwordEncoder);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> authService.register(new RegisterRequest("new_user", "昵称", "abc12345", "abc12346")));

        assertEquals("两次输入的密码不一致", error.getMessage());
    }

    @Test
    void registerShouldRejectWeakPassword() {
        AuthService authService = new AuthService(authenticationManager, jwtTokenService, userMapper, passwordEncoder);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> authService.register(new RegisterRequest("new_user", "昵称", "abcdefgh", "abcdefgh")));

        assertEquals("密码必须为 8-64 位，且同时包含字母和数字", error.getMessage());
    }
}
