package de.mmbbs.kassensystem.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Produkt {
    private final int id;
    private final IntegerProperty idProperty;
    private final StringProperty nameProperty;
    private final DoubleProperty preisProperty;
    private final IntegerProperty lagerbestandProperty;
    private String name;
    private double preis;
    private int lagerbestand;

    public Produkt(int id, String name, double preis, int lagerbestand) {
        this.id = id;
        this.idProperty = new SimpleIntegerProperty(id);
        this.nameProperty = new SimpleStringProperty();
        this.preisProperty = new SimpleDoubleProperty();
        this.lagerbestandProperty = new SimpleIntegerProperty();
        setName(name);
        setPreis(preis);
        setLagerbestand(lagerbestand);
    }

    public int getId() {
        return id;
    }

    public IntegerProperty getIdProperty() {
        return idProperty;
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public DoubleProperty preisProperty() {
        return preisProperty;
    }

    public IntegerProperty lagerbestandProperty() {
        return lagerbestandProperty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Bitte Produktnamen eingeben.");
        }
        this.name = name.trim();
        this.nameProperty.set(this.name);
    }

    public double getPreis() {
        return preis;
    }

    public void setPreis(double preis) {
        if (preis <= 0) {
            throw new IllegalArgumentException("Preis muss größer als 0 sein.");
        }
        this.preis = preis;
        this.preisProperty.set(this.preis);
    }

    public int getLagerbestand() {
        return lagerbestand;
    }

    public void setLagerbestand(int lagerbestand) {
        if (lagerbestand < 0) {
            throw new IllegalArgumentException("Lagerbestand darf nicht negativ sein.");
        }
        this.lagerbestand = lagerbestand;
        this.lagerbestandProperty.set(this.lagerbestand);
    }

    public void bestandErhoehen(int menge) {
        if (menge <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        setLagerbestand(this.lagerbestand + menge);
    }

    public void bestandVerringern(int menge) {
        if (menge <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        if (this.lagerbestand < menge) {
            throw new IllegalArgumentException("Nicht genügend Produkte auf Lager.");
        }
        setLagerbestand(this.lagerbestand - menge);
    }

    @Override
    public String toString() {
        return id + " - " + name + " (" + String.format("%.2f", preis) + " €)";
    }
}
