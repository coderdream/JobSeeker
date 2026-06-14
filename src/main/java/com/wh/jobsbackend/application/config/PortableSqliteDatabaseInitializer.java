package com.wh.jobsbackend.application.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@Component
@Profile("portable")
@ConditionalOnProperty(prefix = "jobs.sqlite.initializer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PortableSqliteDatabaseInitializer implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    private static final int SCHEMA_VERSION = 1;

    private Environment environment;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String dataSourceUrl = environment.getProperty("spring.datasource.url", "");
        if (!dataSourceUrl.startsWith("jdbc:sqlite:")) {
            throw new IllegalStateException("Portable profile requires a SQLite JDBC URL");
        }
        ensureDataDirectory(dataSourceUrl);
        initialize(dataSourceUrl);
    }

    private void ensureDataDirectory(String dataSourceUrl) {
        String databasePath = dataSourceUrl.substring("jdbc:sqlite:".length());
        int queryIndex = databasePath.indexOf('?');
        if (queryIndex >= 0) {
            databasePath = databasePath.substring(0, queryIndex);
        }
        if (databasePath.isBlank() || databasePath.equals(":memory:")) {
            return;
        }
        Path parent = Path.of(databasePath).toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create portable SQLite data directory: " + parent, e);
        }
    }

    private void initialize(String dataSourceUrl) {
        try (Connection connection = DriverManager.getConnection(dataSourceUrl)) {
            applyPragmas(connection);

            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/sqlite/schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/sqlite/data.sql"));

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT OR REPLACE INTO hub_sqlite_schema_version (id, version, description, installed_at)
                        VALUES (1, %d, 'portable sqlite baseline', CURRENT_TIMESTAMP)
                        """.formatted(SCHEMA_VERSION));
            }
            log.info("Portable SQLite database initialized at schema version {}", SCHEMA_VERSION);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize portable SQLite database", e);
        }
    }

    private void applyPragmas(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout=5000");
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA journal_mode=WAL")) {
            if (resultSet.next()) {
                log.debug("Portable SQLite journal mode: {}", resultSet.getString(1));
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
