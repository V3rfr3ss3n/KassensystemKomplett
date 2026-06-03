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
                ensureColumn(stmt, "produkte", "einheit", "TEXT NOT NULL DEFAULT 'STUECK'");
                ensureColumn(stmt, "produkte", "steuerSatz", "REAL NOT NULL DEFAULT 19.0");
                ensureColumn(stmt, "bon_positionen", "steuerSatz", "REAL NOT NULL DEFAULT 19.0");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void ensureColumn(Statement stmt, String table, String column, String definition) {
        try {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (Exception ignored) {
            // SQLite wirft einen Fehler, wenn die Spalte bereits existiert.
        }
    }

    public static String getDbPath() {
        return DB_PATH;
    }
}
