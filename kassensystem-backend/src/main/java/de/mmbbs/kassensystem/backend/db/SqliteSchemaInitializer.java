package de.mmbbs.kassensystem.backend.db;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Legt beim Start des Spring-Backends das SQLite-Schema an.
 *
 * <p>Das Backend benoetigt dieselben Tabellen wie JavaFX, damit beide Teile
 * dieselbe Datenbankdatei verwenden koennen.</p>
 */
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
                        produkt_name TEXT,
                        einheit TEXT,
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
            ensureColumn(statement, "bon_positionen", "produkt_name", "TEXT");
            ensureColumn(statement, "bon_positionen", "einheit", "TEXT");
            execute(statement, "UPDATE bon_positionen SET produkt_name = (SELECT name FROM produkte WHERE id = produkt_id) WHERE produkt_name IS NULL");
            execute(statement, "UPDATE bon_positionen SET einheit = COALESCE((SELECT einheit FROM produkte WHERE id = produkt_id), 'STUECK') WHERE einheit IS NULL");
        }
    }

    private void execute(Statement statement, String sql) throws Exception {
        statement.execute(sql);
    }

    private void ensureColumn(Statement statement, String table, String column, String definition) throws SQLException {
        try (ResultSet spalten = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (spalten.next()) {
                if (column.equalsIgnoreCase(spalten.getString("name"))) {
                    return;
                }
            }
        }
        statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }
}
