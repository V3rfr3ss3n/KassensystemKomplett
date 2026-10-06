package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Produkt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProduktFilterTest {
    @Test
    void findetProduktAuchUeberScanCodeUndGrenztPreisEin() {
        Produkt produkt = new Produkt(3, "Apfel", 2.50, 4);
        produkt.setScanCode("4006381333931");
        assertTrue(new ProduktFilter("400638", null, null, null, "2,00", "3,00", true).passt(produkt));
        assertFalse(new ProduktFilter("400638", null, null, null, "3,00", null, false).passt(produkt));
        assertFalse(new ProduktFilter("falsch", null, null, null, null, null, false).passt(produkt));
    }
}
