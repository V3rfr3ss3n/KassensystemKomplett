package de.mmbbs.kassensystem.backend;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import de.mmbbs.kassensystem.backend.model.ProduktDto;
import de.mmbbs.kassensystem.backend.service.EtikettenPdfService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class EtikettenPdfServiceTest {
    @Test
    void qrCodeKannVomGedrucktenBogenWiederGelesenWerden() throws Exception {
        ProduktDto produkt = new ProduktDto(1, "Apfel", 2.50, 4, null, "STUECK", "Stück", 7, "Obst", "KS-P-000001");
        byte[] pdf = new EtikettenPdfService().erstelle(Collections.nCopies(25, produkt));
        try (var dokument = Loader.loadPDF(pdf)) {
            assertEquals(2, dokument.getNumberOfPages());
            var bild = new PDFRenderer(dokument).renderImage(0, 3);
            int x = (int) ((24 + (595.2756 - 48) / 3 - 64) * 3);
            int y = (int) ((28 + (841.8898 - 56) / 8 - 63) * 3);
            int breite = 60 * 3;
            int[] pixel = bild.getRGB(x, y, breite, breite, null, 0, breite);
            var bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(breite, breite, pixel)));
            assertEquals("KS-P-000001", new MultiFormatReader().decode(bitmap).getText());
        }
    }
}
