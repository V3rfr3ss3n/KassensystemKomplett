package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Verkaufseinheit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanEntscheidungTest {
    @Test
    void stueckUndPackungWerdenDirektHinzugefuegt() {
        assertEquals(ScanEntscheidung.Aktion.HINZUFUEGEN, ScanEntscheidung.pruefe(List.of(produkt(Verkaufseinheit.STUECK, 2)), " 123 ").aktion());
        assertEquals(ScanEntscheidung.Aktion.HINZUFUEGEN, ScanEntscheidung.pruefe(List.of(produkt(Verkaufseinheit.PACKUNG, 2)), "123").aktion());
    }

    @Test
    void gewichtUndVolumenFordernMenge() {
        assertEquals(ScanEntscheidung.Aktion.MENGE_EINGEBEN, ScanEntscheidung.pruefe(List.of(produkt(Verkaufseinheit.KILOGRAMM, 2)), "123").aktion());
        assertEquals(ScanEntscheidung.Aktion.MENGE_EINGEBEN, ScanEntscheidung.pruefe(List.of(produkt(Verkaufseinheit.LITER, 2)), "123").aktion());
    }

    @Test
    void unbekanntUndNichtVerfuegbarSindGetrennteFehler() {
        assertEquals(ScanEntscheidung.Aktion.UNBEKANNT, ScanEntscheidung.pruefe(List.of(produkt(Verkaufseinheit.STUECK, 2)), "x").aktion());
        assertEquals(ScanEntscheidung.Aktion.NICHT_VERFUEGBAR, ScanEntscheidung.pruefe(List.of(produkt(Verkaufseinheit.STUECK, 0)), "123").aktion());
    }

    private Produkt produkt(Verkaufseinheit einheit, int bestand) {
        Produkt produkt = new Produkt(1, "Test", 1, bestand, null, einheit, 19);
        produkt.setScanCode("123");
        return produkt;
    }
}
