package de.mmbbs.kassensystem.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BonPositionTest {

    @Test
    void toString_zeigtProduktMengeUndGesamtpreis() {
        BonPosition position = new BonPosition(new Produkt(1, "Kaffee", 1.20, 10), 3);

        assertEquals("Kaffee x 3 = 3,60 €", position.toString());
    }
}
