package de.mmbbs.kassensystem.model;

import de.mmbbs.kassensystem.util.GeldFormatter;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

/**
 * Fachmodell fuer ein Produkt im Kassensystem.
 *
 * <p>Ein Produkt enthaelt die fuer das Lastenheft geforderten Kerndaten
 * Produktnummer, Name, Preis und Lagerbestand. Zusaetzlich verwaltet das
 * Projekt Einheit, Steuersatz und optional einen Bildpfad.</p>
 */
public class Produkt {
    private final int id;
    private final IntegerProperty idProperty;
    private final StringProperty nameProperty;
    private final DoubleProperty preisProperty;
    private final DoubleProperty lagerbestandProperty;
    private String name;
    private double preis;
    private double lagerbestand;
    private String bildPfad;
    private Verkaufseinheit einheit;
    private double steuerSatz;

    public Produkt(int id, String name, double preis, int lagerbestand) {
        this(id, name, preis, (double) lagerbestand, null);
    }

    public Produkt(int id, String name, double preis, int lagerbestand, String bildPfad) {
        this(id, name, preis, (double) lagerbestand, bildPfad);
    }

    public Produkt(int id, String name, double preis, double lagerbestand, String bildPfad) {
        this(id, name, preis, lagerbestand, bildPfad, Verkaufseinheit.STUECK, Steuersatz.REGELSTEUERSATZ.getProzent());
    }

    /**
     * Erstellt ein vollstaendiges Produktobjekt.
     *
     * @param id Produktnummer; 0 bedeutet, dass das Repository eine neue ID vergibt.
     * @param name Anzeigename des Produkts.
     * @param preis Bruttopreis pro Verkaufseinheit.
     * @param lagerbestand Aktueller Lagerbestand.
     * @param bildPfad Optionaler Pfad zum Produktbild.
     * @param einheit Verkaufseinheit, zum Beispiel Stueck, kg oder l.
     * @param steuerSatz Umsatzsteuersatz in Prozent.
     */
    public Produkt(int id, String name, double preis, double lagerbestand, String bildPfad,
                   Verkaufseinheit einheit, double steuerSatz) {
        this.id = id;
        this.idProperty = new SimpleIntegerProperty(id);
        this.nameProperty = new SimpleStringProperty();
        this.preisProperty = new SimpleDoubleProperty();
        this.lagerbestandProperty = new SimpleDoubleProperty();
        this.bildPfad = bildPfad;
        this.einheit = einheit == null ? Verkaufseinheit.STUECK : einheit;
        this.steuerSatz = Steuersatz.fromProzent(steuerSatz).getProzent();
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

    public DoubleProperty lagerbestandProperty() {
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
        if (!Double.isFinite(preis) || preis <= 0) {
            throw new IllegalArgumentException("Preis muss größer als 0 sein.");
        }
        this.preis = preis;
        this.preisProperty.set(this.preis);
    }

    public double getLagerbestand() {
        return lagerbestand;
    }

    public String getBildPfad() {
        return bildPfad;
    }

    public void setBildPfad(String bildPfad) {
        this.bildPfad = bildPfad;
    }

    public void setLagerbestand(double lagerbestand) {
        if (!Double.isFinite(lagerbestand) || lagerbestand < 0) {
            throw new IllegalArgumentException("Lagerbestand darf nicht negativ sein.");
        }
        this.lagerbestand = lagerbestand;
        this.lagerbestandProperty.set(this.lagerbestand);
    }

    public Verkaufseinheit getEinheit() {
        return einheit;
    }

    public String getEinheitLabel() {
        return einheit.getLabel();
    }

    public void setEinheit(Verkaufseinheit einheit) {
        this.einheit = einheit == null ? Verkaufseinheit.STUECK : einheit;
    }

    public double getSteuerSatz() {
        return steuerSatz;
    }

    public void setSteuerSatz(double steuerSatz) {
        this.steuerSatz = Steuersatz.fromProzent(steuerSatz).getProzent();
    }

    /**
     * Erhoeht den Lagerbestand, zum Beispiel bei einem Warenzugang.
     *
     * @param menge Zuzubuchende Menge; muss groesser als 0 sein.
     */
    public void bestandErhoehen(double menge) {
        if (!Double.isFinite(menge) || menge <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        setLagerbestand(this.lagerbestand + menge);
    }

    /**
     * Verringert den Lagerbestand nach einem Verkauf.
     *
     * @param menge Verkaufte Menge; muss verfuegbar und groesser als 0 sein.
     */
    public void bestandVerringern(double menge) {
        if (!Double.isFinite(menge) || menge <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        if (this.lagerbestand < menge) {
            throw new IllegalArgumentException("Nicht genügend Produkte auf Lager.");
        }
        setLagerbestand(this.lagerbestand - menge);
    }

    @Override
    public String toString() {
        return id + " - " + name + " (" + GeldFormatter.formatiereBetrag(preis) + "/" + getEinheitLabel() + ")";
    }
}
