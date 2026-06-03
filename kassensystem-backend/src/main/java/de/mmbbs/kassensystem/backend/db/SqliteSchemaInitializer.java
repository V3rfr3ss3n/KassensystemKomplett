package de.mmbbs.kassensystem.backend.db;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class SqliteSchemaInitializer implements ApplicationRunner {
    private final DataSource dataSource;

    public SqliteSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            execute(statement, """
                    CREATE TABLE IF NOT EXISTS produkte (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        preis REAL NOT NULL,
                        lagerbestand REAL NOT NULL,
                        bildPfad TEXT,
                        einheit TEXT NOT NULL DEFAULT 'STUECK',
                        steuerSatz REAL NOT NULL DEFAULT 19.0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            execute(statement, """
                    CREATE TABLE IF NOT EXISTS bons (
                        bonnummer INTEGER PRIMARY KEY AUTOINCREMENT,
                        datumUhrzeit TIMESTAMP NOT NULL,
                        gesamtpreis REAL NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            execute(statement, """
                    CREATE TABLE IF NOT EXISTS bon_positionen (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        bonnummer INTEGER NOT NULL,
                        produkt_id INTEGER NOT NULL,
                        menge REAL NOT NULL,
                        einzelpreis REAL NOT NULL,
                        steuerSatz REAL NOT NULL DEFAULT 19.0,
                        gesamtpreis REAL NOT NULL,
                        FOREIGN KEY(bonnummer) REFERENCES bons(bonnummer),
                        FOREIGN KEY(produkt_id) REFERENCES produkte(id)
                    )
                    """);
            ensureColumn(statement, "produkte", "einheit", "TEXT NOT NULL DEFAULT 'STUECK'");
            ensureColumn(statement, "produkte", "steuerSatz", "REAL NOT NULL DEFAULT 19.0");
            ensureColumn(statement, "bon_positionen", "steuerSatz", "REAL NOT NULL DEFAULT 19.0");
        }
    }

    private void execute(Statement statement, String sql) throws Exception {
        statement.execute(sql);
    }

    private void ensureColumn(Statement statement, String table, String column, String definition) {
        try {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (Exception ignored) {
            // SQLite meldet einen Fehler, wenn die Spalte bereits existiert.
        }
    }
}
