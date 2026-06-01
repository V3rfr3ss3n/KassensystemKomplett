package de.mmbbs.kassensystem.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final String DB_PATH = "kassensystem.db";

    public static void initialize() {
        try {
            String url = "jdbc:sqlite:" + DB_PATH;
            try (Connection conn = DriverManager.getConnection(url)) {
                if (conn != null) {
                    createSchema(conn);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createSchema(Connection conn) throws IOException {
        String schemaPath = "src/main/resources/db/migration/V1__Initial_Schema.sql";
        Path path = Paths.get(schemaPath);

        if (!Files.exists(path)) {
            schemaPath = "db/migration/V1__Initial_Schema.sql";
            path = Paths.get(schemaPath);
        }

        if (Files.exists(path)) {
            String schema = Files.readString(path);
            String[] statements = schema.split(";");

            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    sql = sql.trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getDbPath() {
        return DB_PATH;
    }
}
