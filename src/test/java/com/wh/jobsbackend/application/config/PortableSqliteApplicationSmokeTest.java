package com.wh.jobsbackend.application.config;

import com.wh.jobsbackend.application.service.ConfigService;
import com.wh.jobsbackend.application.service.CookieService;
import com.wh.jobsbackend.worker.manager.PlaywrightManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.datasource.username=",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "app.startup.enabled=false",
        "app.cookie-seed.enabled=false",
        "app.zhilian-option-init.enabled=false"
})
@ActiveProfiles("portable")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PortableSqliteApplicationSmokeTest {

    @DynamicPropertySource
    static void portableSqliteProperties(DynamicPropertyRegistry registry) throws Exception {
        Path databasePath = Path.of("target", "portable-smoke-" + UUID.randomUUID() + ".db").toAbsolutePath();
        Files.createDirectories(databasePath.getParent());
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + databasePath);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private PlaywrightManager playwrightManager;

    @MockBean
    private CookieService cookieService;

    @MockBean
    private ConfigService configService;

    @Test
    void portableProfileShouldCreateSqliteDatabaseBeforeApplicationBeansUseIt() {
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='flyway_schema_history'",
                Integer.class
        ));
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hub_city",
                Integer.class
        ) > 0);
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hub_platform_option WHERE platform='51job'",
                Integer.class
        ) > 0);
    }
}
