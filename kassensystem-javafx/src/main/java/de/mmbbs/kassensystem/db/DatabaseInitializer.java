package de.mmbbs.kassensystem.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Initialisiert die gemeinsame SQLite-Datenbank fuer die JavaFX-Anwendung.
 *
 * <p>Die Klasse legt Tabellen an und migriert fehlende Spalten nach, damit
 * bestehende lokale Datenbanken weiter nutzbar bleiben.</p>
 */
public class DatabaseInitializer {
    private static final String DB_PATH_PROPERTY = "kassensystem.db.path";
    private static final String DB_PATH_ENV = "KASSENSYSTEM_DB_PATH";
    private static final Path DB_PATH = resolveDbPath();

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
            // SQLite meldet einen Fehler, wenn die Spalte bereits existiert.
        }
    }

    public static String getDbPath() {
        return DB_PATH.toString();
    }

    private static Path resolveDbPath() {
        String configuredPath = System.getProperty(DB_PATH_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv(DB_PATH_ENV);
        }

        Path path;
        if (configuredPath != null && !configuredPath.isBlank()) {
            path = Paths.get(configuredPath);
        } else {
            Path workspace = workspaceRoot();
            path = workspace.resolve("data").resolve("kassensystem.db");
            copyExistingDatabaseIfNeeded(workspace, path);
        }

        path = path.toAbsolutePath().normalize();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Datenbankordner konnte nicht erstellt werden: " + path, e);
        }
        return path;
    }

    private static Path workspaceRoot() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path fileName = cwd.getFileName();
        if (fileName != null && fileName.toString().startsWith("kassensystem-") && cwd.getParent() != null) {
            return cwd.getParent();
        }
        return cwd;
    }

    private static void copyExistingDatabaseIfNeeded(Path workspace, Path target) {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (Files.exists(normalizedTarget)) {
            return;
        }

        Path[] candidates = {
                workspace.resolve("kassensystem-javafx").resolve("kassensystem.db"),
                workspace.resolve("kassensystem-backend").resolve("kassensystem.db"),
                workspace.resolve("kassensystem.db")
        };

        for (Path candidate : candidates) {
            Path normalizedCandidate = candidate.toAbsolutePath().normalize();
            if (normalizedCandidate.equals(normalizedTarget) || !Files.exists(normalizedCandidate)) {
                continue;
            }
            try {
                Files.createDirectories(normalizedTarget.getParent());
                Files.copy(normalizedCandidate, normalizedTarget, StandardCopyOption.COPY_ATTRIBUTES);
                return;
            } catch (IOException ignored) {
                // Wenn die Uebernahme fehlschlaegt, wird die gemeinsame DB neu angelegt.
            }
        }
    }
}
