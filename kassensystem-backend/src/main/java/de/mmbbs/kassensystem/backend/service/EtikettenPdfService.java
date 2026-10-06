package de.mmbbs.kassensystem.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import de.mmbbs.kassensystem.backend.model.ProduktDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EtikettenPdfService {
    private static final int SPALTEN = 3;
    private static final int ZEILEN = 8;
    private static final float RAND_X = 24;
    private static final float RAND_Y = 28;
    private static final float BREITE = (PDRectangle.A4.getWidth() - 2 * RAND_X) / SPALTEN;
    private static final float HOEHE = (PDRectangle.A4.getHeight() - 2 * RAND_Y) / ZEILEN;
    private static final PDType1Font SCHRIFT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    public byte[] erstelle(List<ProduktDto> produkte) {
        try (PDDocument dokument = new PDDocument(); ByteArrayOutputStream ausgabe = new ByteArrayOutputStream()) {
            for (int start = 0; start < produkte.size(); start += SPALTEN * ZEILEN) {
                PDPage seite = new PDPage(PDRectangle.A4);
                dokument.addPage(seite);
                try (PDPageContentStream stream = new PDPageContentStream(dokument, seite)) {
                    for (int i = 0; i < SPALTEN * ZEILEN && start + i < produkte.size(); i++) {
                        zeichneEtikett(stream, produkte.get(start + i), i);
                    }
                }
            }
            dokument.save(ausgabe);
            return ausgabe.toByteArray();
        } catch (IOException | WriterException ex) {
            throw new IllegalStateException("Etiketten konnten nicht erzeugt werden.", ex);
        }
    }

    private void zeichneEtikett(PDPageContentStream stream, ProduktDto produkt, int position)
            throws IOException, WriterException {
        int spalte = position % SPALTEN;
        int zeile = position / SPALTEN;
        float x = RAND_X + spalte * BREITE;
        float oben = PDRectangle.A4.getHeight() - RAND_Y - zeile * HOEHE;
        stream.setStrokingColor(0.75f);
        stream.addRect(x + 2, oben - HOEHE + 2, BREITE - 4, HOEHE - 4);
        stream.stroke();

        String name = pdfText(produkt.name());
        while (name.length() > 1 && SCHRIFT.getStringWidth(name) / 1000 * 9 > BREITE - 16) {
            name = name.substring(0, name.length() - 1);
        }
        schreibe(stream, name, x + 8, oben - 14, 9, true);
        schreibe(stream, pdfText(String.format(Locale.GERMANY, "%.2f EUR", produkt.preis())), x + 8, oben - 27, 8, false);

        BitMatrix qr = new QRCodeWriter().encode(produkt.scanCode(), BarcodeFormat.QR_CODE, 0, 0,
                Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                        EncodeHintType.MARGIN, 1,
                        EncodeHintType.CHARACTER_SET, "UTF-8"));
        float pixel = Math.min(53f / qr.getWidth(), 53f / qr.getHeight());
        float qrX = x + BREITE - 62;
        float qrY = oben - HOEHE + 8;
        stream.setNonStrokingColor(0f);
        for (int py = 0; py < qr.getHeight(); py++) {
            for (int px = 0; px < qr.getWidth(); px++) {
                if (qr.get(px, py)) stream.addRect(qrX + px * pixel, qrY + (qr.getHeight() - py - 1) * pixel, pixel, pixel);
            }
        }
        stream.fill();
        String code = pdfText(produkt.scanCode());
        int zeichenProZeile = 30;
        int zeilen = (code.length() + zeichenProZeile - 1) / zeichenProZeile;
        if (zeilen > 6) zeichenProZeile = (code.length() + 5) / 6;
        for (int i = 0, textZeile = 0; i < code.length(); i += zeichenProZeile, textZeile++) {
            schreibe(stream, code.substring(i, Math.min(i + zeichenProZeile, code.length())),
                    x + 8, oben - 38 - textZeile * 7, 5, false);
        }
    }

    private static void schreibe(PDPageContentStream stream, String text, float x, float y, int groesse, boolean fett)
            throws IOException {
        stream.beginText();
        stream.setFont(new PDType1Font(fett ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA), groesse);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private static String pdfText(String text) {
        return text == null ? "" : text.replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", "?");
    }
}
