package de.mmbbs.kassensystem.model;

import de.mmbbs.kassensystem.util.GeldFormatter;
import de.mmbbs.kassensystem.util.MengenFormatter;

public class BonPosition {
    private final Produkt produkt;
    private final double menge;
    private final double einzelpreis;
    private final double gesamtpreis;
    private final double steuerSatz;

    public BonPosition(Produkt produkt, double menge) {
        this(produkt, menge, produkt == null ? 0 : produkt.getPreis(), Double.NaN,
                produkt == null ? 19.0 : produkt.getSteuerSatz());
    }

    public BonPosition(Produkt produkt, double menge, double einzelpreis, double gesamtpreis, double steuerSatz) {
        if (produkt == null) {
            throw new IllegalArgumentException("Bitte Produkt auswählen.");
        }
        if (menge <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        this.produkt = produkt;
        this.menge = menge;
        this.einzelpreis = einzelpreis;
        this.gesamtpreis = Double.isNaN(gesamtpreis) ? this.einzelpreis * menge : gesamtpreis;
        this.steuerSatz = Steuersatz.fromProzent(steuerSatz).getProzent();
    }

    public Produkt getProdukt() {
        return produkt;
    }

    public double getMenge() {
        return menge;
    }

    public double getEinzelpreis() {
        return einzelpreis;
    }

    public double getGesamtpreis() {
        return gesamtpreis;
    }

    public double getSteuerSatz() {
        return steuerSatz;
    }

    public double getNettoGesamtpreis() {
        return gesamtpreis / (1 + steuerSatz / 100.0);
    }

    public double getSteuerbetrag() {
        return gesamtpreis - getNettoGesamtpreis();
    }

    @Override
    public String toString() {
        return produkt.getName()
                + " x " + MengenFormatter.formatiereMenge(menge, produkt.getEinheitLabel())
                + " = " + GeldFormatter.formatiereBetrag(gesamtpreis);
    }
}
