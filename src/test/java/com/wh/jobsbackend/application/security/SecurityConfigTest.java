package com.wh.jobsbackend.application.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void securityShouldPermitAsyncAndErrorDispatches() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/wh/jobsbackend/application/security/SecurityConfig.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("dispatcherTypeMatchers"));
        assertTrue(source.contains("DispatcherType.ERROR"));
        assertTrue(source.contains("DispatcherType.ASYNC"));
    }

    @Test
    void securityShouldPermitPackagedFrontendRoutes() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/wh/jobsbackend/application/security/SecurityConfig.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("\"/\""));
        assertTrue(source.contains("\"/_next/**\""));
        assertTrue(source.contains("\"/*.html\""));
        assertTrue(source.contains("\"/*.txt\""));
        assertTrue(source.contains("\"/login\""));
        assertTrue(source.contains("\"/boss/**\""));
        assertTrue(source.contains("\"/zhilian/**\""));
        assertTrue(source.contains(".anyRequest().authenticated()"));
    }
}
