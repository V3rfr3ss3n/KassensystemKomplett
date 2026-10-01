package de.mmbbs.kassensystem.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Ermittelt den gemeinsamen SQLite-Pfad fuer Spring Boot.
 *
 * <p>Ohne explizite Konfiguration wird `data/kassensystem.db` im Workspace
 * genutzt. Dadurch greifen JavaFX und Backend auf dieselbe Datenbasis zu.</p>
 */
public final class DatabasePathResolver {
    public static final String DB_PATH_PROPERTY = "kassensystem.db.path";
    private static final String DB_PATH_ENV = "KASSENSYSTEM_DB_PATH";

    private DatabasePathResolver() {
    }

    /**
     * Liefert den absoluten Pfad zur SQLite-Datenbank und legt den Ordner an.
     */
    public static Path resolve() {
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

    /**
     * Liefert die JDBC-URL fuer Spring Boot.
     */
    public static String jdbcUrl() {
        return "jdbc:sqlite:" + resolve();
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
