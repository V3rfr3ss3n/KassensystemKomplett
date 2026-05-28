package de.mmbbs.kassensystem.model;

public class BonPosition {
    private final Produkt produkt;
    private final int menge;
    private final double einzelpreis;
    private final double gesamtpreis;

    public BonPosition(Produkt produkt, int menge) {
        if (produkt == null) {
            throw new IllegalArgumentException("Bitte Produkt auswählen.");
        }
        if (menge <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        this.produkt = produkt;
        this.menge = menge;
        this.einzelpreis = produkt.getPreis();
        this.gesamtpreis = this.einzelpreis * menge;
    }

    public Produkt getProdukt() {
        return produkt;
    }

    public int getMenge() {
        return menge;
    }

    public double getEinzelpreis() {
        return einzelpreis;
    }

    public double getGesamtpreis() {
        return gesamtpreis;
    }
}
