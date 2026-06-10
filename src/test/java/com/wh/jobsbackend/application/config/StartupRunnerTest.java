package com.wh.jobsbackend.application.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupRunnerTest {

    @Test
    void startupRunnerShouldGatePlaywrightInitializationBehindProperty() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/wh/jobsbackend/application/config/StartupRunner.java"));

        assertTrue(source.contains("app.startup.playwright.enabled"));
        assertTrue(source.contains("initializePlaywright"));
        assertTrue(source.contains("if (initializePlaywright)"));
    }
}
