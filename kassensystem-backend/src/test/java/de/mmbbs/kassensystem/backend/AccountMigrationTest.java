package de.mmbbs.kassensystem.backend;

import de.mmbbs.kassensystem.backend.db.SqliteSchemaInitializer;
import de.mmbbs.kassensystem.backend.security.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountMigrationTest {
    @TempDir Path temp;

    @Test
    void benutzermigrationErhaeltAlteProdukteUndBons() throws Exception {
        var source = new DriverManagerDataSource("jdbc:sqlite:" + temp.resolve("old.db"));
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE produkte(id INTEGER PRIMARY KEY, name TEXT, preis REAL, lagerbestand REAL, bildPfad TEXT, einheit TEXT, steuerSatz REAL)");
        jdbc.execute("CREATE TABLE bons(bonnummer INTEGER PRIMARY KEY, datumUhrzeit TEXT, gesamtpreis REAL)");
        jdbc.execute("CREATE TABLE bon_positionen(id INTEGER PRIMARY KEY, bonnummer INTEGER, produkt_id INTEGER, menge REAL, einzelpreis REAL, steuerSatz REAL, gesamtpreis REAL)");
        jdbc.update("INSERT INTO produkte VALUES(1,'Apfel',2,7,NULL,'STUECK',19)");
        jdbc.update("INSERT INTO bons VALUES(1,'2026-10-02T12:00:00',2)");
        jdbc.update("INSERT INTO bon_positionen VALUES(1,1,1,1,2,19,2)");

        new SqliteSchemaInitializer(source).run(null);

        assertEquals("Apfel", jdbc.queryForObject("SELECT name FROM produkte WHERE id=1", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM bons", Integer.class));
        assertEquals("Apfel", jdbc.queryForObject("SELECT produkt_name FROM bon_positionen WHERE id=1", String.class));
        assertEquals("KS-P-000001", jdbc.queryForObject("SELECT scan_code FROM produkte WHERE id=1", String.class));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("INSERT INTO produkte(name,preis,lagerbestand,scan_code) VALUES('Kopie',1,1,'KS-P-000001')"));
        new SqliteSchemaInitializer(source).run(null);
        assertEquals("KS-P-000001", jdbc.queryForObject("SELECT scan_code FROM produkte WHERE id=1", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM benutzer", Integer.class));

        var accounts = new AccountService(jdbc, new BCryptPasswordEncoder(), false, "EigenesStartpasswort123");
        accounts.run(null);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM benutzer", Integer.class));
        assertEquals(true, accounts.findByLogin("admin").mustChange());
        assertEquals(true, new BCryptPasswordEncoder().matches("EigenesStartpasswort123", accounts.findByLogin("admin").hash()));
        accounts.run(null);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM benutzer", Integer.class));
    }

}
