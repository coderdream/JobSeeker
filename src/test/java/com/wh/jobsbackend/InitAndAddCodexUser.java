package com.wh.jobsbackend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class InitAndAddCodexUser {
    public static void main(String[] args) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Codex12345");
        
        String url = "jdbc:sqlite:db/getjobs.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            // Run V1__init.sql
            String sqlScript = new String(Files.readAllBytes(Paths.get("src/main/resources/db/sqlite/V1__init.sql")));
            try (Statement stmt = conn.createStatement()) {
                String[] queries = sqlScript.split(";");
                for (String q : queries) {
                    if (q.trim().length() > 0) {
                        stmt.execute(q);
                    }
                }
            }
            System.out.println("Schema initialized!");

            String checkSql = "SELECT count(*) FROM hub_user WHERE username = 'codex'";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                java.sql.ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("User already exists!");
                    return;
                }
            }
            
            String sql = "INSERT INTO hub_user(username, password_hash, nickname, role, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "codex");
                pstmt.setString(2, hash);
                pstmt.setString(3, "Codex User");
                pstmt.setString(4, "ADMIN");
                pstmt.setString(5, "ACTIVE");
                
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                pstmt.setString(6, now);
                pstmt.setString(7, now);
                pstmt.executeUpdate();
                System.out.println("User inserted successfully!");
            }
        }
    }
}
