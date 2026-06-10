package com.wh.jobsbackend.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wh.jobsbackend.application.dto.LoginResponse;
import com.wh.jobsbackend.application.dto.RegisterResponse;
import com.wh.jobsbackend.application.security.CurrentUserService;
import com.wh.jobsbackend.application.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {
    private MockMvc mockMvc;
    private AuthService authService;
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        currentUserService = mock(CurrentUserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, currentUserService)).build();
    }

    @Test
    void loginShouldReturnTokenPayload() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse(1L, "alice", "jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new java.util.HashMap<>() {{
                            put("username", "alice");
                            put("password", "secret");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void registerShouldReturnSuccessPayload() throws Exception {
        when(authService.register(any())).thenReturn(new RegisterResponse(true, "注册成功，请登录", 2L, "new_user", "新用户"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new java.util.HashMap<>() {{
                            put("username", "new_user");
                            put("nickname", "新用户");
                            put("password", "abc12345");
                            put("confirmPassword", "abc12345");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("注册成功，请登录"))
                .andExpect(jsonPath("$.username").value("new_user"))
                .andExpect(jsonPath("$.nickname").value("新用户"));
    }

    @Test
    void meShouldReturnCurrentUser() throws Exception {
        when(currentUserService.requireUserId()).thenReturn(9L);
        when(currentUserService.requireUsername()).thenReturn("bob");
        when(currentUserService.requireRole()).thenReturn("USER");

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void logoutShouldReturnSuccessPayload() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
