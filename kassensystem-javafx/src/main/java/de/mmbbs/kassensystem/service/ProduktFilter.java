package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.util.GeldFormatter;

import java.util.Locale;

/** Filterkriterien der Produktauswahl, ohne Abhängigkeit zu JavaFX-Controls. */
public record ProduktFilter(String suche, String einheit, String kategorie, String steuer,
                            String preisVon, String preisBis, boolean nurVerfuegbar) {
    public boolean passt(Produkt produkt) {
        if (suche != null && !suche.isBlank()) {
            String text = (produkt.getId() + " " + produkt.getName() + " " + produkt.getScanCode() + " "
                    + produkt.getKategorie() + " " + produkt.getEinheitLabel() + " "
                    + GeldFormatter.formatiereBetrag(produkt.getPreis())).toLowerCase(Locale.ROOT);
            if (!text.contains(suche.trim().toLowerCase(Locale.ROOT))) return false;
        }
        if (einheit != null && !"Alle Einheiten".equals(einheit) && !produkt.getEinheitLabel().equals(einheit)) return false;
        if (kategorie != null && !"Alle Kategorien".equals(kategorie) && !produkt.getKategorie().equals(kategorie)) return false;
        if (steuer != null && !"Alle Steuersätze".equals(steuer)
                && !String.format("%.0f %%", produkt.getSteuerSatz()).equals(steuer)) return false;
        double von = optionaleZahl(preisVon);
        double bis = optionaleZahl(preisBis);
        if (!Double.isNaN(von) && produkt.getPreis() < von) return false;
        if (!Double.isNaN(bis) && produkt.getPreis() > bis) return false;
        return !nurVerfuegbar || produkt.getLagerbestand() > 0;
    }

    private static double optionaleZahl(String text) {
        if (text == null || text.isBlank()) return Double.NaN;
        try { return Double.parseDouble(text.trim().replace(',', '.')); }
        catch (NumberFormatException ex) { return Double.NaN; }
    }
}
