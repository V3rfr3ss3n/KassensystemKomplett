package de.mmbbs.kassensystem.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Initialisiert die gemeinsame SQLite-Datenbank fuer die JavaFX-Anwendung.
 *
 * <p>Die Klasse legt Tabellen an und migriert fehlende Spalten nach, damit
 * bestehende lokale Datenbanken weiter nutzbar bleiben.</p>
 */
public class DatabaseInitializer {
    private static final String DB_PATH_PROPERTY = "kassensystem.db.path";
    private static final String DB_PATH_ENV = "KASSENSYSTEM_DB_PATH";

    public static void initialize() {
        initialize(Path.of(getDbPath()));
    }

    public static void initialize(Path datenbankPfad) {
        try {
            String url = "jdbc:sqlite:" + datenbankPfad.toAbsolutePath().normalize();
            try (Connection conn = DriverManager.getConnection(url)) {
                if (conn != null) {
                    createSchema(conn);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Datenbank konnte nicht initialisiert werden.", e);
        }
    }

    private static void createSchema(Connection conn) throws IOException {
        String schemaPath = "src/main/resources/db/migration/V1__Initial_Schema.sql";
        Path path = Paths.get(schemaPath);

        if (!Files.exists(path)) {
            schemaPath = "db/migration/V1__Initial_Schema.sql";
            path = Paths.get(schemaPath);
        }

        String schema;
        if (Files.exists(path)) {
            schema = Files.readString(path);
        } else {
            try (var stream = DatabaseInitializer.class.getResourceAsStream("/db/migration/V1__Initial_Schema.sql")) {
                if (stream == null) {
                    throw new IOException("Datenbankschema fehlt.");
                }
                schema = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
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
            ensureColumn(stmt, "bon_positionen", "produkt_name", "TEXT");
            ensureColumn(stmt, "bon_positionen", "einheit", "TEXT");
            stmt.executeUpdate("UPDATE bon_positionen SET produkt_name = (SELECT name FROM produkte WHERE id = produkt_id) WHERE produkt_name IS NULL");
            stmt.executeUpdate("UPDATE bon_positionen SET einheit = COALESCE((SELECT einheit FROM produkte WHERE id = produkt_id), 'STUECK') WHERE einheit IS NULL");
        } catch (SQLException e) {
            throw new IllegalStateException("Datenbankschema konnte nicht vorbereitet werden.", e);
        }
    }

    private static void ensureColumn(Statement stmt, String table, String column, String definition) throws SQLException {
        try (ResultSet spalten = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (spalten.next()) {
                if (column.equalsIgnoreCase(spalten.getString("name"))) {
                    return;
                }
            }
        }
        stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    public static String getDbPath() {
        return resolveDbPath().toString();
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
