package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.util.GeldFormatter;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class BonService {
    private static final DateTimeFormatter DATUM_UHRZEIT_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm 'Uhr'");
    private int naechsteBonNummer = 1;

    public Bon erstelleBon(List<BonPosition> positionen) {
        if (positionen == null || positionen.isEmpty()) {
            throw new IllegalArgumentException("Der Warenkorb ist leer.");
        }
        return new Bon(naechsteBonNummer++, positionen);
    }

    public String formatiereBon(Bon bon) {
        StringBuilder builder = new StringBuilder();
        builder.append("========================================\n");
        builder.append("Bon Nr. ").append(bon.getBonnummer()).append("\n");
        builder.append("Datum: ").append(DATUM_UHRZEIT_FORMAT.format(bon.getDatumUhrzeit())).append("\n");
        builder.append("----------------------------------------\n");
        builder.append(String.format("%-18s %5s %10s%n", "Produkt", "Menge", "Gesamt"));
        builder.append("----------------------------------------\n");
        for (BonPosition position : bon.getPositionen()) {
            builder.append(String.format("%-18s %5d %10s%n",
                    kuerzeProduktname(position.getProdukt().getName()),
                    position.getMenge(),
                    GeldFormatter.formatiereBetrag(position.getGesamtpreis())));
        }
        builder.append("----------------------------------------\n");
        builder.append(String.format("%-24s %14s%n", "Gesamt:", GeldFormatter.formatiereBetrag(bon.getGesamtpreis())));
        builder.append("========================================");
        return builder.toString();
    }

    private String kuerzeProduktname(String name) {
        if (name.length() <= 18) {
            return name;
        }
        return name.substring(0, 15) + "...";
    }
}
