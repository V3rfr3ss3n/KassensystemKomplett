package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BonServiceTest {

    @Test
    void formatiereBon_erstelltLesbarenBonMitFormatiertenGeldbetraegen() {
        Produkt kaffee = new Produkt(1, "Kaffee", 1.20, 10);
        Produkt kuchen = new Produkt(2, "Kuchen", 1.50, 5);
        Bon bon = new Bon(
                7,
                LocalDateTime.of(2026, 6, 3, 14, 5),
                List.of(new BonPosition(kaffee, 2), new BonPosition(kuchen, 1)),
                Double.NaN
        );

        String text = new BonService().formatiereBon(bon);

        assertTrue(text.contains("Bon Nr. 7"));
        assertTrue(text.contains("03.06.2026 14:05 Uhr"));
        assertTrue(text.contains("Produkt"));
        assertTrue(text.contains("Kaffee"));
        assertTrue(text.contains("2,40 €"));
        assertTrue(text.contains("Gesamt:"));
        assertTrue(text.contains("3,90 €"));
    }
}
