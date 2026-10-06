package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Verkaufseinheit;

import java.util.List;
import java.util.Optional;

/** Entscheidungslogik für Tastatur-Scanner, unabhängig von JavaFX-Steuerelementen. */
public final class ScanEntscheidung {
    public enum Aktion { HINZUFUEGEN, MENGE_EINGEBEN, UNBEKANNT, NICHT_VERFUEGBAR }
    public record Ergebnis(Aktion aktion, Produkt produkt) { }

    private ScanEntscheidung() { }

    public static Ergebnis pruefe(List<Produkt> katalog, String eingabe) {
        String code = eingabe == null ? "" : eingabe.trim();
        Optional<Produkt> gefunden = katalog.stream()
                .filter(produkt -> produkt.getScanCode() != null && produkt.getScanCode().equals(code))
                .findFirst();
        if (gefunden.isEmpty() || code.isEmpty()) return new Ergebnis(Aktion.UNBEKANNT, null);
        Produkt produkt = gefunden.get();
        if (produkt.getLagerbestand() <= 0) return new Ergebnis(Aktion.NICHT_VERFUEGBAR, produkt);
        if (produkt.getEinheit() == Verkaufseinheit.KILOGRAMM || produkt.getEinheit() == Verkaufseinheit.LITER) {
            return new Ergebnis(Aktion.MENGE_EINGEBEN, produkt);
        }
        return new Ergebnis(Aktion.HINZUFUEGEN, produkt);
    }
}
