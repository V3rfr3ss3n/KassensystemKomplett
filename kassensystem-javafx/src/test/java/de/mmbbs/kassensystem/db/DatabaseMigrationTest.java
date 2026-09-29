package de.mmbbs.kassensystem.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseMigrationTest {
    @TempDir
    Path ordner;

    @Test
    void alteBonpositionenErhaltenProduktnameUndEinheit() throws Exception {
        Path pfad = ordner.resolve("alt.db");
        String url = "jdbc:sqlite:" + pfad;
        try (var verbindung = DriverManager.getConnection(url);
             var statement = verbindung.createStatement()) {
            statement.execute("CREATE TABLE produkte (id INTEGER PRIMARY KEY, name TEXT, preis REAL, lagerbestand REAL, bildPfad TEXT, einheit TEXT, steuerSatz REAL)");
            statement.execute("CREATE TABLE bons (bonnummer INTEGER PRIMARY KEY, datumUhrzeit TEXT, gesamtpreis REAL)");
            statement.execute("CREATE TABLE bon_positionen (id INTEGER PRIMARY KEY, bonnummer INTEGER, produkt_id INTEGER, menge REAL, einzelpreis REAL, steuerSatz REAL, gesamtpreis REAL)");
            statement.execute("INSERT INTO produkte VALUES (1, 'Alter Tee', 1.5, 2, NULL, 'PACKUNG', 7)");
            statement.execute("INSERT INTO bon_positionen VALUES (1, 1, 1, 1, 1.5, 7, 1.5)");
        }

        DatabaseInitializer.initialize(pfad);

        try (var verbindung = DriverManager.getConnection(url);
             var statement = verbindung.createStatement();
             var daten = statement.executeQuery("SELECT produkt_name, einheit FROM bon_positionen WHERE id = 1")) {
            assertEquals("Alter Tee", daten.getString("produkt_name"));
            assertEquals("PACKUNG", daten.getString("einheit"));
        }
    }
}
