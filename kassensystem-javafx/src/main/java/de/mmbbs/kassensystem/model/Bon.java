package de.mmbbs.kassensystem.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unveraenderlicher Kassenbon eines abgeschlossenen Kaufs.
 *
 * <p>Der Bon speichert Bonnummer, Zeitpunkt, Positionen und Gesamtpreis. Er
 * dient sowohl fuer die Anzeige in JavaFX als auch fuer die Bon-Historie.</p>
 */
public class Bon {
    private final int bonnummer;
    private final LocalDateTime datumUhrzeit;
    private final List<BonPosition> positionen;
    private final double gesamtpreis;

    public Bon(int bonnummer, List<BonPosition> positionen) {
        this(bonnummer, LocalDateTime.now(), positionen, Double.NaN);
    }

    /**
     * Erstellt einen Bon mit festem Zeitpunkt und optional vorgegebenem Gesamtpreis.
     *
     * @param bonnummer Fortlaufende Bonnummer.
     * @param datumUhrzeit Zeitpunkt des Kaufabschlusses.
     * @param positionen Gekaufte Warenpositionen.
     * @param gesamtpreis Gesamtpreis; bei {@link Double#NaN} wird er berechnet.
     */
    public Bon(int bonnummer, LocalDateTime datumUhrzeit, List<BonPosition> positionen, double gesamtpreis) {
        this.bonnummer = bonnummer;
        this.datumUhrzeit = datumUhrzeit;
        this.positionen = new ArrayList<>(positionen);
        this.gesamtpreis = Double.isNaN(gesamtpreis)
                ? this.positionen.stream().mapToDouble(BonPosition::getGesamtpreis).sum()
                : gesamtpreis;
    }

    public int getBonnummer() {
        return bonnummer;
    }

    public LocalDateTime getDatumUhrzeit() {
        return datumUhrzeit;
    }

    public List<BonPosition> getPositionen() {
        return List.copyOf(positionen);
    }

    public double getGesamtpreis() {
        return gesamtpreis;
    }
}
