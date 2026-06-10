package com.wh.jobsbackend.application.encoding;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChineseEncodingRegressionTest {

    private static final List<String> BAD_MARKERS = List.of(
            "\uFFFD", "锟", "闁", "閺", "鏇", "鍒", "鐢", "娉", "浠", "璇", "鎴", "鍚"
    );

    @Test
    void runtimeStringLiteralsShouldNotContainMojibakeMarkers() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        List<String> violations = new ArrayList<>();

        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> collectViolations(path, violations));
        }

        assertThat(violations).isEmpty();
    }

    private static void collectViolations(Path path, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!looksLikeRuntimeString(line)) {
                    continue;
                }
                for (String marker : BAD_MARKERS) {
                    if (line.contains(marker)) {
                        violations.add(path + ":" + (i + 1) + ": " + line.trim());
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            violations.add(path + ": " + ex.getMessage());
        }
    }

    private static boolean looksLikeRuntimeString(String line) {
        String trimmed = line.trim();
        return trimmed.contains("\"")
                && !trimmed.startsWith("//")
                && !trimmed.startsWith("*")
                && !trimmed.startsWith("/*");
    }
}
