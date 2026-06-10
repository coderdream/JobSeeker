package com.wh.jobsbackend.application.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtTokenServiceTest {

    @Test
    void shouldGenerateTokenWithExpectedClaims() {
        JwtTokenService jwtTokenService = new JwtTokenService(
                "change-me-to-a-64-byte-secret-change-me-to-a-64-byte-secret",
                "jobs-backend",
                7200
        );
        AppUserPrincipal principal = new AppUserPrincipal(7L, "alice", "hash", "ADMIN", "ACTIVE");

        String token = jwtTokenService.generateToken(principal);
        Claims claims = jwtTokenService.parseToken(token);

        assertNotNull(token);
        assertEquals("alice", claims.getSubject());
        assertEquals("jobs-backend", claims.getIssuer());
        assertEquals(7, ((Number) claims.get("uid")).intValue());
        assertEquals("ADMIN", claims.get("role"));
    }
}
