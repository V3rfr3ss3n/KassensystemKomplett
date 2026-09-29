package de.mmbbs.kassensystem.model;

import de.mmbbs.kassensystem.util.GeldFormatter;
import de.mmbbs.kassensystem.util.MengenFormatter;

/**
 * Einzelne Position auf einem Kassenbon oder im Warenkorb.
 *
 * <p>Die Position haelt Produkt, Menge, Einzelpreis, Gesamtpreis und den
 * Umsatzsteuersatz als Snapshot fest, damit alte Bons auch nach
 * Produktpreis-Aenderungen nachvollziehbar bleiben.</p>
 */
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

    /**
     * Erstellt eine Bonposition mit expliziten Preis- und Steuerwerten.
     *
     * @param produkt Produkt der Position.
     * @param menge Gekaufte Menge.
     * @param einzelpreis Einzelpreis zum Kaufzeitpunkt.
     * @param gesamtpreis Gesamtpreis; bei {@link Double#NaN} wird er berechnet.
     * @param steuerSatz Umsatzsteuersatz in Prozent.
     */
    public BonPosition(Produkt produkt, double menge, double einzelpreis, double gesamtpreis, double steuerSatz) {
        if (produkt == null) {
            throw new IllegalArgumentException("Bitte Produkt auswählen.");
        }
        if (!Double.isFinite(menge) || menge <= 0 || !Double.isFinite(einzelpreis) || einzelpreis <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        this.produkt = new Produkt(produkt.getId(), produkt.getName(), einzelpreis, 0,
                produkt.getBildPfad(), produkt.getEinheit(), produkt.getSteuerSatz());
        this.menge = menge;
        this.einzelpreis = einzelpreis;
        this.gesamtpreis = Double.isNaN(gesamtpreis) ? this.einzelpreis * menge : gesamtpreis;
        if (!Double.isFinite(this.gesamtpreis) || this.gesamtpreis <= 0) {
            throw new IllegalArgumentException("Positionsbetrag ist ungueltig.");
        }
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

    /**
     * Berechnet den Nettoanteil der Position aus dem Bruttogesamtpreis.
     */
    public double getNettoGesamtpreis() {
        return gesamtpreis / (1 + steuerSatz / 100.0);
    }

    /**
     * Berechnet den Umsatzsteueranteil der Position.
     */
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
