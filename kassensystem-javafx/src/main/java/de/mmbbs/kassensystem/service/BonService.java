package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.util.GeldFormatter;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BonService {
    private static final DateTimeFormatter DATUM_UHRZEIT_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm 'Uhr'");
    private int naechsteBonNummer = 1;

    public BonService() {
    }

    public BonService(int naechsteBonNummer) {
        this.naechsteBonNummer = Math.max(1, naechsteBonNummer);
    }

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
        builder.append(String.format("%-14s %7s %4s %10s%n", "Produkt", "Menge", "USt", "Brutto"));
        builder.append("----------------------------------------\n");
        for (BonPosition position : bon.getPositionen()) {
            builder.append(String.format("%-14s %7s %4s %10s%n",
                    kuerzeProduktname(position.getProdukt().getName()),
                    de.mmbbs.kassensystem.util.MengenFormatter.formatiereMenge(position.getMenge()),
                    formatiereSteuersatz(position.getSteuerSatz()),
                    GeldFormatter.formatiereBetrag(position.getGesamtpreis())));
        }
        builder.append("----------------------------------------\n");
        for (SteuerSumme steuerSumme : berechneSteuerSummen(bon).values()) {
            builder.append(String.format("%-10s Netto %10s%n",
                    formatiereSteuersatz(steuerSumme.steuerSatz()),
                    GeldFormatter.formatiereBetrag(steuerSumme.netto())));
            builder.append(String.format("%-10s USt   %10s%n",
                    formatiereSteuersatz(steuerSumme.steuerSatz()),
                    GeldFormatter.formatiereBetrag(steuerSumme.steuer())));
        }
        builder.append(String.format("%-24s %14s%n", "Gesamt:", GeldFormatter.formatiereBetrag(bon.getGesamtpreis())));
        builder.append("========================================");
        return builder.toString();
    }

    private String kuerzeProduktname(String name) {
        if (name.length() <= 14) {
            return name;
        }
        return name.substring(0, 11) + "...";
    }

    private Map<Double, SteuerSumme> berechneSteuerSummen(Bon bon) {
        Map<Double, SteuerSumme> summen = new LinkedHashMap<>();
        for (BonPosition position : bon.getPositionen()) {
            summen.compute(position.getSteuerSatz(), (steuerSatz, bestehend) -> {
                if (bestehend == null) {
                    return new SteuerSumme(steuerSatz, position.getNettoGesamtpreis(), position.getSteuerbetrag());
                }
                return new SteuerSumme(
                        steuerSatz,
                        bestehend.netto() + position.getNettoGesamtpreis(),
                        bestehend.steuer() + position.getSteuerbetrag()
                );
            });
        }
        return summen;
    }

    private String formatiereSteuersatz(double steuerSatz) {
        return String.format(java.util.Locale.GERMANY, "%.0f%%", steuerSatz);
    }

    private record SteuerSumme(double steuerSatz, double netto, double steuer) {
    }
}
