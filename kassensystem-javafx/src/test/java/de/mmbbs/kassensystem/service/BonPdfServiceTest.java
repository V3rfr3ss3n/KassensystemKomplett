package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BonPdfServiceTest {
    static {
        System.setProperty("pdfbox.fontcache", System.getProperty("java.io.tmpdir"));
    }

    @TempDir
    Path ordner;

    @Test
    void exportEnthaeltBonnummerPositionEinzelpreisSteuerUndSumme() throws Exception {
        Bon bon = new Bon(42, List.of(new BonPosition(new Produkt(1, "Kaffee", 2.5, 10), 2)));
        Path datei = ordner.resolve("bon-42.pdf");

        new BonPdfService().exportiere(bon, datei);

        try (var dokument = Loader.loadPDF(datei.toFile())) {
            String text = new PDFTextStripper().getText(dokument);
            assertTrue(text.contains("Bon Nr. 42"));
            assertTrue(text.contains("Kaffee"));
            assertTrue(text.contains("2,50 EUR"));
            assertTrue(text.contains("19%"));
            assertTrue(text.contains("5,00 EUR"));
        }
    }
}
