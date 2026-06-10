package com.wh.jobsbackend.application.service;

import com.wh.jobsbackend.application.entity.PlatformOptionTypeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformOptionTypeServiceTest {

    private JdbcTemplate jdbcTemplate;
    private ReferenceDataService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:option_type_" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        jdbcTemplate = new JdbcTemplate(dataSource);
        service = new ReferenceDataService(jdbcTemplate);
    }

    @Test
    void platformOptionTypesShouldUseConfigurableTypeCatalog() {
        jdbcTemplate.execute("""
                CREATE TABLE hub_platform_option_type (
                    id BIGINT PRIMARY KEY,
                    platform VARCHAR(50) NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    label VARCHAR(100) NOT NULL,
                    sort_order INTEGER,
                    enabled INTEGER DEFAULT 1
                )
                """);
        jdbcTemplate.update("INSERT INTO hub_platform_option_type (id, platform, type, label, sort_order, enabled) VALUES (1, 'boss', 'industry', 'Industry industry', 20, 1)");
        jdbcTemplate.update("INSERT INTO hub_platform_option_type (id, platform, type, label, sort_order, enabled) VALUES (2, 'boss', 'salary', 'Salary salary', 10, 1)");
        jdbcTemplate.update("INSERT INTO hub_platform_option_type (id, platform, type, label, sort_order, enabled) VALUES (3, 'boss', 'hidden', 'Hidden hidden', 30, 0)");

        List<PlatformOptionTypeEntity> enabledTypes = service.listPlatformOptionTypes("boss", true);
        List<PlatformOptionTypeEntity> allTypes = service.listPlatformOptionTypes("boss", null);

        assertEquals(2, enabledTypes.size());
        assertEquals(2L, enabledTypes.get(0).getId());
        assertEquals("boss", enabledTypes.get(0).getPlatform());
        assertEquals("salary", enabledTypes.get(0).getType());
        assertEquals("Salary salary", enabledTypes.get(0).getLabel());
        assertEquals(10, enabledTypes.get(0).getSortOrder());
        assertEquals(1, enabledTypes.get(0).getEnabled());
        assertEquals(1L, enabledTypes.get(1).getId());
        assertEquals("industry", enabledTypes.get(1).getType());
        assertEquals(3, allTypes.size());
    }

    @Test
    void missingPlatformOptionTypeTableShouldReturnEmptyValuesForFallbackCallers() {
        assertTrue(service.listPlatformOptionTypes("boss", true).isEmpty());
    }
}
