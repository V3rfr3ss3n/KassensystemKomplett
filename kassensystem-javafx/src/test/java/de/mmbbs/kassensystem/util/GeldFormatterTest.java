package de.mmbbs.kassensystem.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeldFormatterTest {

    @Test
    void formatiereBetrag_nutztDeutschesEuroFormat() {
        assertEquals("1.234,50 €", GeldFormatter.formatiereBetrag(1234.5));
    }

    @Test
    void formatiereZahl_nutztKommaOhneTausendertrennzeichen() {
        assertEquals("1234,50", GeldFormatter.formatiereZahl(1234.5));
    }
}
