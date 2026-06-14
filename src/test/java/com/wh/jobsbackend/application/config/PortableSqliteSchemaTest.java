package com.wh.jobsbackend.application.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortableSqliteSchemaTest {

    @Test
    void sqliteSchemaAndSeedShouldInitializePortableDatabase() throws Exception {
        Path databasePath = Path.of("target", "portable-sqlite-schema-" + UUID.randomUUID() + ".db").toAbsolutePath();
        Files.createDirectories(databasePath.getParent());

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + databasePath);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/sqlite/schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/sqlite/data.sql"));
        }

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name",
                String.class
        );

        assertTrue(tables.contains("hub_user"));
        assertTrue(tables.contains("hub_config"));
        assertTrue(tables.contains("hub_cookie"));
        assertTrue(tables.contains("hub_ai"));
        assertTrue(tables.contains("hub_boss_config"));
        assertTrue(tables.contains("hub_job51_data"));
        assertTrue(tables.contains("hub_liepin_data"));
        assertTrue(tables.contains("hub_zhilian_data"));
        assertTrue(tables.contains("hub_yupao_data"));
        assertTrue(tables.contains("hub_platform_option_type"));
        assertTrue(tables.contains("hub_sqlite_schema_version"));

        assertEquals(5, jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT platform) FROM hub_city_platform_code",
                Integer.class
        ));
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hub_platform_option WHERE platform='boss' AND type='salary'",
                Integer.class
        ) > 0);
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hub_platform_option_type WHERE platform='liepin'",
                Integer.class
        ) > 0);

        String schema = readClasspath("db/sqlite/schema.sql");
        String seed = readClasspath("db/sqlite/data.sql");
        String combined = schema + "\n" + seed;
        assertFalse(combined.contains("BIGSERIAL"));
        assertFalse(combined.contains("DO $$"));
        assertFalse(combined.contains("ARRAY["));
        assertFalse(combined.contains("ALTER TABLE IF EXISTS"));
        assertFalse(combined.contains("ADD COLUMN IF NOT EXISTS"));
    }

    private static String readClasspath(String path) throws Exception {
        return new ClassPathResource(path)
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
