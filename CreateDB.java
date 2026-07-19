import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CreateDB {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://192.168.1.5:5432/postgres";
        String user = "coderdream";
        String password = "codex-local-20260715";
        
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            System.out.println("Connected to PostgreSQL server.");
            stmt.executeUpdate("CREATE DATABASE appdb");
            System.out.println("Database 'appdb' created successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
