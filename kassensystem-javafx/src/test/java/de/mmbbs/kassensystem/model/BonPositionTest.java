package de.mmbbs.kassensystem.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BonPositionTest {

    @Test
    void toString_zeigtProduktMengeUndGesamtpreis() {
        BonPosition position = new BonPosition(new Produkt(1, "Kaffee", 1.20, 10), 3);

        assertEquals("Kaffee x 3 Stück = 3,60 €", position.toString());
    }

    @Test
    void steuerbetrag_wirdAusBruttopreisUndSteuersatzBerechnet() {
        Produkt apfel = new Produkt(2, "Apfel", 1.50, 10, null,
                Verkaufseinheit.KILOGRAMM, Steuersatz.ERMAESSIGT.getProzent());
        BonPosition position = new BonPosition(apfel, 2.0);

        assertEquals(3.00, position.getGesamtpreis(), 0.001);
        assertEquals(2.80, position.getNettoGesamtpreis(), 0.01);
        assertEquals(0.20, position.getSteuerbetrag(), 0.01);
    }
}
