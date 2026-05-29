package de.mmbbs.kassensystem.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Bon {
    private final int bonnummer;
    private final LocalDateTime datumUhrzeit;
    private final List<BonPosition> positionen;
    private final double gesamtpreis;

    public Bon(int bonnummer, List<BonPosition> positionen) {
        this(bonnummer, LocalDateTime.now(), positionen, Double.NaN);
    }

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
