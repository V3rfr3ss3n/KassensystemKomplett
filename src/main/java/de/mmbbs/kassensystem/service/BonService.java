package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;

import java.util.List;

public class BonService {
    private int naechsteBonNummer = 1;

    public Bon erstelleBon(List<BonPosition> positionen) {
        if (positionen == null || positionen.isEmpty()) {
            throw new IllegalArgumentException("Der Warenkorb ist leer.");
        }
        return new Bon(naechsteBonNummer++, positionen);
    }

    public String formatiereBon(Bon bon) {
        StringBuilder builder = new StringBuilder();
        builder.append("Bon Nr. ").append(bon.getBonnummer()).append("\n");
        builder.append("Datum: ").append(bon.getDatumUhrzeit().toLocalDate()).append("\n");
        builder.append("Uhrzeit: ").append(bon.getDatumUhrzeit().toLocalTime()).append("\n\n");
        builder.append(String.format("%-12s %-8s %-10s %-8s%n", "Produkt", "Menge", "Preis", "Gesamt"));
        for (BonPosition position : bon.getPositionen()) {
            builder.append(String.format("%-12s %-8d %-10.2f %-8.2f%n",
                    position.getProdukt().getName(),
                    position.getMenge(),
                    position.getEinzelpreis(),
                    position.getGesamtpreis()));
        }
        builder.append("\nGesamtpreis: ").append(String.format("%.2f €", bon.getGesamtpreis()));
        return builder.toString();
    }
}
