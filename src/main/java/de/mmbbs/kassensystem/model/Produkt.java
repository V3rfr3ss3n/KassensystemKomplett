package de.mmbbs.kassensystem.model;

public class Produkt {
    private final int id;
    private String name;
    private double preis;
    private int lagerbestand;

    public Produkt(int id, String name, double preis, int lagerbestand) {
        this.id = id;
        setName(name);
        setPreis(preis);
        setLagerbestand(lagerbestand);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Bitte Produktnamen eingeben.");
        }
        this.name = name.trim();
    }

    public double getPreis() {
        return preis;
    }

    public void setPreis(double preis) {
        if (preis <= 0) {
            throw new IllegalArgumentException("Preis muss größer als 0 sein.");
        }
        this.preis = preis;
    }

    public int getLagerbestand() {
        return lagerbestand;
    }

    public void setLagerbestand(int lagerbestand) {
        if (lagerbestand < 0) {
            throw new IllegalArgumentException("Lagerbestand darf nicht negativ sein.");
        }
        this.lagerbestand = lagerbestand;
    }

    public void bestandErhoehen(int menge) {
        if (menge <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        this.lagerbestand += menge;
    }

    public void bestandVerringern(int menge) {
        if (menge <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        if (this.lagerbestand < menge) {
            throw new IllegalArgumentException("Nicht genügend Produkte auf Lager.");
        }
        this.lagerbestand -= menge;
    }

    @Override
    public String toString() {
        return id + " - " + name + " (" + String.format("%.2f", preis) + " €)";
    }
}
