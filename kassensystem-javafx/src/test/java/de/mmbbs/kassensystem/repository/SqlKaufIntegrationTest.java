package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.service.KassenService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlKaufIntegrationTest {
    static {
        System.setProperty("kassensystem.db.path", Path.of("target", "test-kauf-" + UUID.randomUUID() + ".db").toString());
    }

    @Test
    void fehlgeschlagenerBonRolltLagerabbuchungZurueckUndSnapshotBleibtNachLoeschenErhalten() throws Exception {
        SqlProduktRepository produkte = new SqlProduktRepository();
        SqlBonHistorieRepository bons = new SqlBonHistorieRepository(produkte);
        Produkt produkt = produkte.speichern(new Produkt(0, "Testkaffee", 2.5, 4));
        KassenService kasse = new KassenService(produkte, bons);
        kasse.positionHinzufuegen(produkt.getId(), 2);

        String url = "jdbc:sqlite:" + de.mmbbs.kassensystem.db.DatabaseInitializer.getDbPath();
        try (var verbindung = DriverManager.getConnection(url);
             var statement = verbindung.createStatement()) {
            statement.execute("CREATE TRIGGER test_bon_fehler BEFORE INSERT ON bons BEGIN SELECT RAISE(FAIL, 'Testfehler'); END");
        }

        assertThrows(IllegalStateException.class, kasse::kassenvorgangAbschliessen);
        assertEquals(4, produkte.findeNachId(produkt.getId()).orElseThrow().getLagerbestand());
        assertTrue(bons.ladeBonHistorie().isEmpty());
        assertEquals(1, kasse.getWarenkorb().size());

        try (var verbindung = DriverManager.getConnection(url);
             var statement = verbindung.createStatement()) {
            statement.execute("DROP TRIGGER test_bon_fehler");
        }

        Bon bon = kasse.kassenvorgangAbschliessen();
        assertEquals(1, bon.getBonnummer());
        assertEquals(2, produkte.findeNachId(produkt.getId()).orElseThrow().getLagerbestand());
        Produkt bearbeitet = produkte.findeNachId(produkt.getId()).orElseThrow();
        bearbeitet.setName("Umbenannter Kaffee");
        produkte.speichern(bearbeitet);
        assertEquals("Testkaffee", bons.ladeBonHistorie().get(0).getPositionen().get(0).getProdukt().getName());
        produkte.loeschen(produkt.getId());
        Bon geladen = new SqlBonHistorieRepository(produkte).ladeBonHistorie().get(0);
        assertEquals("Testkaffee", geladen.getPositionen().get(0).getProdukt().getName());
        assertEquals(2.5, geladen.getPositionen().get(0).getEinzelpreis());
    }
}
