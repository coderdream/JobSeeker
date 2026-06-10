package com.wh.jobsbackend.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceDataServiceTest {

    private JdbcTemplate jdbcTemplate;
    private ReferenceDataService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:reference_" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        jdbcTemplate = new JdbcTemplate(dataSource);
        service = new ReferenceDataService(jdbcTemplate);
    }

    @Test
    void cityOptionsShouldUseSharedCityAndPlatformCodeTables() {
        jdbcTemplate.execute("""
                CREATE TABLE hub_city (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    city_code VARCHAR(100),
                    sort_order INTEGER,
                    enabled INTEGER DEFAULT 1
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE hub_city_platform_code (
                    id BIGINT PRIMARY KEY,
                    city_id BIGINT NOT NULL,
                    platform VARCHAR(50) NOT NULL,
                    platform_city_code VARCHAR(100) NOT NULL,
                    platform_city_name VARCHAR(100),
                    enabled INTEGER DEFAULT 1
                )
                """);
        jdbcTemplate.update("INSERT INTO hub_city (id, name, city_code, sort_order, enabled) VALUES (1, ?, 'beijing', 20, 1)", "北京");
        jdbcTemplate.update("INSERT INTO hub_city (id, name, city_code, sort_order, enabled) VALUES (2, ?, 'shanghai', 10, 1)", "上海");
        jdbcTemplate.update("INSERT INTO hub_city_platform_code (id, city_id, platform, platform_city_code, platform_city_name, enabled) VALUES (11, 1, 'boss', '101010100', ?, 1)", "北京");
        jdbcTemplate.update("INSERT INTO hub_city_platform_code (id, city_id, platform, platform_city_code, platform_city_name, enabled) VALUES (12, 2, 'boss', '101020100', ?, 1)", "上海");

        List<ReferenceDataService.OptionItem> items = service.listCityOptionsForPlatform("boss");

        assertEquals(2, items.size());
        assertEquals(new ReferenceDataService.OptionItem(12L, "city", "上海", "101020100", 10), items.get(0));
        assertEquals(new ReferenceDataService.OptionItem(11L, "city", "北京", "101010100", 20), items.get(1));
        assertEquals("101010100", service.codeByName("boss", "city", "北京"));
        assertEquals("上海", service.nameByCode("boss", "city", "101020100"));
    }

    @Test
    void platformOptionsShouldUseSharedPlatformOptionTable() {
        jdbcTemplate.execute("""
                CREATE TABLE hub_platform_option (
                    id BIGINT PRIMARY KEY,
                    platform VARCHAR(50) NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    code VARCHAR(100) NOT NULL,
                    sort_order INTEGER,
                    enabled INTEGER DEFAULT 1
                )
                """);
        jdbcTemplate.update("INSERT INTO hub_platform_option (id, platform, type, name, code, sort_order, enabled) VALUES (1, 'job51', 'salary', ?, '01', 2, 1)", "3千以下");
        jdbcTemplate.update("INSERT INTO hub_platform_option (id, platform, type, name, code, sort_order, enabled) VALUES (2, 'job51', 'salary', ?, '02', 1, 1)", "3-5千");

        List<ReferenceDataService.OptionItem> items = service.listPlatformOptionItems("job51", "salary");

        assertEquals(2, items.size());
        assertEquals(new ReferenceDataService.OptionItem(2L, "salary", "3-5千", "02", 1), items.get(0));
        assertEquals("02", service.codeByName("job51", "salary", "3-5千"));
        assertEquals("3千以下", service.nameByCode("job51", "salary", "01"));
    }

    @Test
    void missingReferenceTablesShouldReturnEmptyValuesForFallbackCallers() {
        assertTrue(service.listCityOptionsForPlatform("boss").isEmpty());
        assertTrue(service.listPlatformOptionItems("boss", "salary").isEmpty());
        assertNull(service.codeByName("boss", "city", "北京"));
        assertNull(service.nameByCode("boss", "city", "101010100"));
    }
}
