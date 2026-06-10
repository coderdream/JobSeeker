package com.wh.jobsbackend.application.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserIsolationMigrationTest {

    @Test
    void migrationScriptsShouldExist() {
        assertTrue(new ClassPathResource("db/migration/V20260419_01__create_user_and_auth_tables.sql").exists());
        assertTrue(new ClassPathResource("db/migration/V20260419_02__add_user_id_to_private_tables.sql").exists());
        assertTrue(new ClassPathResource("db/migration/V20260419_03__backfill_legacy_admin.sql").exists());
        assertTrue(new ClassPathResource("db/migration/V20260602_01__add_running_task_guard.sql").exists());
        assertTrue(new ClassPathResource("db/migration/V20260602_02__move_runtime_tables_to_flyway.sql").exists());
        assertTrue(new ClassPathResource("db/migration/V20260603_01__create_missing_private_tables.sql").exists());
        assertTrue(new ClassPathResource("db/migration/V20260604_01__create_reference_data_tables.sql").exists());
        assertTrue(new ClassPathResource("db/migration/V20260604_02__normalize_table_names.sql").exists());
        assertTrue(new ClassPathResource("db/migration/V20260607_01__add_yupao_platform.sql").exists());
        assertTrue(new ClassPathResource("db/migration/V20260609_01__add_liepin_search_filters.sql").exists());
    }

    @Test
    void tableNameNormalizationMigrationShouldRenameApplicationTablesToHubPrefix() throws IOException {
        String migration = new ClassPathResource("db/migration/V20260604_02__normalize_table_names.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        Map<String, String> renames = Map.ofEntries(
                Map.entry("app_user", "hub_user"),
                Map.entry("user_job_task", "hub_user_job_task"),
                Map.entry("config", "hub_config"),
                Map.entry("cookie", "hub_cookie"),
                Map.entry("ai", "hub_ai"),
                Map.entry("boss_config", "hub_boss_config"),
                Map.entry("job51_config", "hub_job51_config"),
                Map.entry("liepin_config", "hub_liepin_config"),
                Map.entry("zhilian_config", "hub_zhilian_config"),
                Map.entry("boss_data", "hub_boss_data"),
                Map.entry("job51_data", "hub_job51_data"),
                Map.entry("liepin_data", "hub_liepin_data"),
                Map.entry("zhilian_data", "hub_zhilian_data"),
                Map.entry("boss_option", "hub_boss_option"),
                Map.entry("job51_option", "hub_job51_option"),
                Map.entry("liepin_option", "hub_liepin_option"),
                Map.entry("zhilian_option", "hub_zhilian_option"),
                Map.entry("boss_industry", "hub_boss_industry"),
                Map.entry("city", "hub_city"),
                Map.entry("city_platform_code", "hub_city_platform_code"),
                Map.entry("platform_option", "hub_platform_option"),
                Map.entry("boss_blacklist", "hub_boss_blacklist"));

        renames.forEach((oldName, newName) -> {
            assertTrue(migration.contains("'" + oldName + "'"), oldName + " should be guarded");
            assertTrue(migration.contains("'" + newName + "'"), newName + " should be guarded");
        });
        assertTrue(migration.contains("to_regclass('public.' || old_table) IS NOT NULL"));
        assertTrue(migration.contains("to_regclass('public.' || new_table) IS NULL"));
        assertTrue(migration.contains("'ALTER TABLE ' || old_table || ' RENAME TO ' || new_table"));
    }

    @Test
    void applicationJavaShouldReferenceHubPrefixedTablesOnly() throws IOException {
        List<String> legacyTableNames = List.of(
                "app_user",
                "user_job_task",
                "config",
                "cookie",
                "ai",
                "boss_config",
                "job51_config",
                "liepin_config",
                "zhilian_config",
                "yupao_config",
                "boss_data",
                "job51_data",
                "liepin_data",
                "zhilian_data",
                "yupao_data",
                "boss_option",
                "job51_option",
                "liepin_option",
                "zhilian_option",
                "boss_industry",
                "city",
                "city_platform_code",
                "platform_option",
                "boss_blacklist");

        try (Stream<Path> paths = Files.walk(Path.of("src/main/java/com/wh/jobsbackend/application"))) {
            List<Path> javaFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path file : javaFiles) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                for (String legacyTableName : legacyTableNames) {
                    assertTrue(!content.contains("@TableName(\"" + legacyTableName + "\")"),
                            file + " should not reference legacy table " + legacyTableName);
                    assertTrue(!Pattern.compile("\\b(FROM|JOIN|INTO|UPDATE|TABLE)\\s+" + legacyTableName + "\\b",
                                    Pattern.CASE_INSENSITIVE).matcher(content).find(),
                            file + " should not reference legacy table " + legacyTableName);
                }
            }
        }
    }

    @Test
    void legacyTableMigrationShouldGuardMissingTablesForEmptyDatabaseBootstrap() throws IOException {
        String migration = new ClassPathResource("db/migration/V20260419_02__add_user_id_to_private_tables.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(migration.contains("ALTER TABLE IF EXISTS cookie"),
                "cookie 表迁移需要允许空库首启时表不存在");
        assertTrue(migration.contains("ALTER TABLE IF EXISTS config"),
                "config 表迁移需要允许空库首启时表不存在");
        assertTrue(migration.contains("ALTER TABLE IF EXISTS ai"),
                "ai 表迁移需要允许空库首启时表不存在");
    }

    @Test
    void backfillMigrationShouldSkipUpdatesWhenLegacyTablesAreMissing() throws IOException {
        String migration = new ClassPathResource("db/migration/V20260419_03__backfill_legacy_admin.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(migration.contains("to_regclass('public.cookie')"),
                "空库首启时 backfill 需要跳过不存在的 cookie 表");
        assertTrue(migration.contains("to_regclass('public.config')"),
                "空库首启时 backfill 需要跳过不存在的 config 表");
        assertTrue(migration.contains("to_regclass('public.ai')"),
                "空库首启时 backfill 需要跳过不存在的 ai 表");
    }

    @Test
    void runningTaskMigrationShouldGuardDuplicateUserPlatformRuns() throws IOException {
        String migration = new ClassPathResource("db/migration/V20260602_01__add_running_task_guard.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(migration.contains("uk_user_job_task_running"));
        assertTrue(migration.contains("user_id, platform"));
        assertTrue(migration.contains("WHERE status = 'RUNNING'"));
    }

    @Test
    void runtimeTableMigrationShouldUsePostgresIdentityInsteadOfSqliteAutoincrement() throws IOException {
        String migration = new ClassPathResource("db/migration/V20260602_02__move_runtime_tables_to_flyway.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS job51_option"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS job51_data"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS zhilian_option"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS zhilian_data"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS liepin_data"));
        assertTrue(!migration.contains("AUTOINCREMENT"));
    }

    @Test
    void zhilianOptionInitializerShouldUsePostgresCompatibleDdl() throws IOException {
        String initializer = Files.readString(
                Path.of("src/main/java/com/wh/jobsbackend/application/init/ZhilianOptionInitializer.java"),
                StandardCharsets.UTF_8);

        assertTrue(!initializer.contains("AUTOINCREMENT"));
        assertTrue(initializer.contains("hub_zhilian_option schema is managed by Flyway"));
        assertTrue(!initializer.contains("CREATE TABLE"));
    }

    @Test
    void missingPrivateTableMigrationShouldCreateCoreTablesForEmptyDatabase() throws IOException {
        String migration = new ClassPathResource("db/migration/V20260603_01__create_missing_private_tables.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS config"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS cookie"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS ai"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS boss_config"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS boss_data"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS boss_blacklist"));
        assertTrue(migration.contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_user"));
        assertTrue(migration.contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_boss_config_user"));
        assertTrue(!migration.contains("AUTOINCREMENT"));
    }

    @Test
    void yupaoMigrationShouldCreatePrivateTablesAndReferenceData() throws IOException {
        String migration = new ClassPathResource("db/migration/V20260607_01__add_yupao_platform.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS hub_yupao_config"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS hub_yupao_data"));
        assertTrue(migration.contains("user_id BIGINT REFERENCES hub_user(id)"));
        assertTrue(migration.contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_yupao_config_user"));
        assertTrue(migration.contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_yupao_data_user_job"));
        assertTrue(migration.contains("'yupao'"));
        assertTrue(migration.contains("hub_city_platform_code"));
        assertTrue(migration.contains("hub_platform_option"));
        assertTrue(!migration.contains("AUTOINCREMENT"));
    }

    @Test
    void liepinFilterMigrationShouldKeepConfigUserScopedAndAddFilterColumns() throws IOException {
        String migration = new ClassPathResource("db/migration/V20260609_01__add_liepin_search_filters.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(migration.contains("ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS user_id"));
        assertTrue(migration.contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_liepin_config_user"));
        for (String column : List.of(
                "comp_tag",
                "pub_time",
                "work_year_code",
                "edu_level",
                "industry",
                "job_kind",
                "comp_scale",
                "comp_stage",
                "comp_kind"
        )) {
            assertTrue(migration.contains("ALTER TABLE IF EXISTS hub_liepin_config ADD COLUMN IF NOT EXISTS " + column),
                    column + " should be added to hub_liepin_config");
        }
    }

    @Test
    void missingPrivateTableMigrationShouldExecuteInPostgresMode() throws Exception {
        String migration = new ClassPathResource("db/migration/V20260603_01__create_missing_private_tables.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:missing_private_tables;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE app_user (id BIGSERIAL PRIMARY KEY)");
            for (String sql : migration.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }

            assertTableExists(connection, "AI");
            assertTableExists(connection, "BOSS_CONFIG");
            assertTableExists(connection, "CONFIG");
            assertTableExists(connection, "COOKIE");
        }
    }

    private void assertTableExists(Connection connection, String tableName) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName, null)) {
            assertTrue(tables.next(), tableName + " table should exist");
        }
    }
}
