package com.wh.jobsbackend.application.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceDataMigrationTest {

    @Test
    void referenceDataMigrationShouldCreateGlobalCityAndPlatformOptionTables() throws Exception {
        String migration = new ClassPathResource("db/migration/V20260604_01__create_reference_data_tables.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS city"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS city_platform_code"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS platform_option"));
        assertTrue(migration.contains("uk_city_city_code"));
        assertTrue(migration.contains("uk_city_platform_code_platform_code"));
        assertTrue(migration.contains("uk_platform_option_platform_type_code"));
    }

    @Test
    void referenceDataMigrationShouldExecuteInPostgresModeAndSeedDefaults() throws Exception {
        String migration = new ClassPathResource("db/migration/V20260604_01__create_reference_data_tables.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:reference_data_tables;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE boss_option (id BIGSERIAL PRIMARY KEY, type VARCHAR(50), name VARCHAR(100), code VARCHAR(100), sort_order INTEGER, created_at TIMESTAMP, updated_at TIMESTAMP)");
            statement.execute("CREATE TABLE job51_option (id BIGSERIAL PRIMARY KEY, type VARCHAR(50), name VARCHAR(100), code VARCHAR(100), sort_order INTEGER, created_at TIMESTAMP, updated_at TIMESTAMP)");
            statement.execute("CREATE TABLE liepin_option (id BIGSERIAL PRIMARY KEY, type VARCHAR(50), name VARCHAR(100), code VARCHAR(100), sort_order INTEGER, created_at TIMESTAMP, updated_at TIMESTAMP)");
            statement.execute("CREATE TABLE zhilian_option (id BIGSERIAL PRIMARY KEY, type VARCHAR(50), name VARCHAR(100), code VARCHAR(100), sort_order INTEGER, created_at TIMESTAMP, updated_at TIMESTAMP)");

            for (String sql : migration.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }

            assertTableExists(connection, "CITY");
            assertTableExists(connection, "CITY_PLATFORM_CODE");
            assertTableExists(connection, "PLATFORM_OPTION");
            assertCountAtLeast(connection, "SELECT COUNT(*) FROM city", 1);
            assertCountAtLeast(connection, "SELECT COUNT(*) FROM city_platform_code WHERE platform = 'boss'", 1);
            assertCountAtLeast(connection, "SELECT COUNT(*) FROM platform_option WHERE platform = 'boss' AND type = 'salary'", 1);
        }
    }

    @Test
    void liepinFilterMigrationShouldSeedAdditionalPlatformOptionTypes() throws Exception {
        String migration = new ClassPathResource("db/migration/V20260609_01__add_liepin_search_filters.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:liepin_filter_options;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE hub_user (id BIGSERIAL PRIMARY KEY)");
            statement.execute("CREATE TABLE hub_liepin_config (id BIGSERIAL PRIMARY KEY)");
            statement.execute("CREATE TABLE hub_platform_option (id BIGSERIAL PRIMARY KEY, platform VARCHAR(50), type VARCHAR(50), name VARCHAR(100), code VARCHAR(100), sort_order INTEGER, enabled INTEGER, created_at TIMESTAMP, updated_at TIMESTAMP)");

            for (String sql : migration.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }

            for (String type : List.of(
                    "compTag",
                    "pubTime",
                    "workYearCode",
                    "degree",
                    "industry",
                    "jobType",
                    "scale",
                    "stage",
                    "compKind"
            )) {
                assertCountAtLeast(connection,
                        "SELECT COUNT(*) FROM hub_platform_option WHERE platform = 'liepin' AND type = '" + type + "'",
                        2);
            }
        }
    }

    @Test
    void platformOptionTypeMigrationShouldCreateAndSeedConfigurableTypeCatalog() throws Exception {
        String migration = new ClassPathResource("db/migration/V20260610_01__create_platform_option_type_catalog.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:platform_option_type_catalog;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE hub_platform_option (id BIGSERIAL PRIMARY KEY, platform VARCHAR(50), type VARCHAR(50), name VARCHAR(100), code VARCHAR(100), sort_order INTEGER, enabled INTEGER, created_at TIMESTAMP, updated_at TIMESTAMP)");

            for (String sql : migration.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }

            assertTableExists(connection, "HUB_PLATFORM_OPTION_TYPE");
            assertCountAtLeast(connection,
                    "SELECT COUNT(*) FROM hub_platform_option_type WHERE platform = 'boss' AND type = 'salary' AND label = '薪资 salary'",
                    1);
            assertCountAtLeast(connection,
                    "SELECT COUNT(*) FROM hub_platform_option_type WHERE platform = 'liepin' AND type = 'compKind'",
                    1);
            assertCountAtLeast(connection,
                    "SELECT COUNT(*) FROM hub_platform_option_type WHERE platform = 'zhilian' AND type = 'city'",
                    1);
        }
    }

    private void assertTableExists(Connection connection, String tableName) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName, null)) {
            assertTrue(tables.next(), tableName + " table should exist");
        }
    }

    private void assertCountAtLeast(Connection connection, String sql, int minimum) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            assertTrue(resultSet.getInt(1) >= minimum, sql + " should return at least " + minimum);
        }
    }
}
