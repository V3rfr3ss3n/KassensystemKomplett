package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Erstellt PDF und Druckausgabe aus derselben Bon-Darstellung wie die Kasse. */
public class BonPdfService {
    private static final int ZEICHEN_PRO_ZEILE = 82;
    private static final float ZEILENHOEHE = 14;
    private final BonService bonService = new BonService();

    public void exportiere(Bon bon, Path ziel) throws IOException {
        try (PDDocument dokument = erstelleDokument(bon)) {
            dokument.save(ziel.toFile());
        }
    }

    /** Gibt false zurueck, wenn der Druckdialog abgebrochen wurde. */
    public boolean drucke(Bon bon) throws IOException, PrinterException {
        try (PDDocument dokument = erstelleDokument(bon)) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Bon " + bon.getBonnummer());
            job.setPageable(new PDFPageable(dokument));
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        }
    }

    private PDDocument erstelleDokument(Bon bon) throws IOException {
        PDDocument dokument = new PDDocument();
        try {
            List<String> zeilen = new ArrayList<>();
            for (String zeile : bonService.formatiereBon(bon).split("\\R")) {
                String pdfZeile = zeile.replace("€", "EUR").replace('·', '-');
                do {
                    int ende = Math.min(pdfZeile.length(), ZEICHEN_PRO_ZEILE);
                    zeilen.add(pdfZeile.substring(0, ende));
                    pdfZeile = pdfZeile.substring(ende);
                } while (!pdfZeile.isEmpty());
            }

            PDPageContentStream inhalt = null;
            float y = 0;
            try {
                for (String zeile : zeilen) {
                    if (inhalt == null || y < 48) {
                        if (inhalt != null) {
                            inhalt.close();
                        }
                        PDPage seite = new PDPage(PDRectangle.A4);
                        dokument.addPage(seite);
                        inhalt = new PDPageContentStream(dokument, seite);
                        inhalt.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 9);
                        y = seite.getMediaBox().getHeight() - 52;
                    }
                    inhalt.beginText();
                    inhalt.newLineAtOffset(48, y);
                    inhalt.showText(zeile);
                    inhalt.endText();
                    y -= ZEILENHOEHE;
                }
            } finally {
                if (inhalt != null) {
                    inhalt.close();
                }
            }
            return dokument;
        } catch (IOException | RuntimeException e) {
            dokument.close();
            throw e;
        }
    }
}
