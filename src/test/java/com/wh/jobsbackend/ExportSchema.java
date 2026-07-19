package com.wh.jobsbackend;

import org.flywaydb.core.Flyway;
import org.h2.tools.Script;
import java.sql.Connection;
import java.sql.DriverManager;

public class ExportSchema {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
        String user = "sa";
        String password = "";

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load();
        
        flyway.migrate();

        Connection conn = DriverManager.getConnection(url, user, password);
        Script.process(conn, "schema_dump.sql", "", "");
        System.out.println("Schema dumped to schema_dump.sql");
    }
}
