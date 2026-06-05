package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Steuersatz;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Verkaufseinheit;
import de.mmbbs.kassensystem.repository.ProduktRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Fachlogik fuer Produktanlage, Produktpflege und Warenzugang.
 *
 * <p>Der Service prueft Eingaben ueber das Produktmodell, schreibt ueber das
 * Repository und benachrichtigt UI-Ansichten, wenn sich Produktdaten aendern.</p>
 */
public class ProduktService {
    private final ProduktRepository repository;
    private final List<ProductChangeListener> listeners = new ArrayList<>();

    public ProduktService(ProduktRepository repository) {
        this.repository = repository;
    }

    public void addListener(ProductChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ProductChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (ProductChangeListener listener : listeners) {
            listener.onProductsChanged();
        }
    }

    public Produkt produktHinzufuegen(String name, double preis, int anfangsbestand) {
        return produktHinzufuegen(name, preis, anfangsbestand, null);
    }

    public Produkt produktHinzufuegen(String name, double preis, int anfangsbestand, String bildPfad) {
        return produktHinzufuegen(name, preis, anfangsbestand, bildPfad,
                Verkaufseinheit.STUECK, Steuersatz.REGELSTEUERSATZ.getProzent());
    }

    /**
     * Legt ein neues Produkt mit allen verwalteten Feldern an.
     *
     * @param name Produktname.
     * @param preis Bruttopreis pro Einheit.
     * @param anfangsbestand Anfangsbestand im Lager.
     * @param bildPfad Optionaler Bildpfad.
     * @param einheit Verkaufseinheit.
     * @param steuerSatz Umsatzsteuer in Prozent.
     * @return Gespeichertes Produkt inklusive Produktnummer.
     */
    public Produkt produktHinzufuegen(String name, double preis, double anfangsbestand, String bildPfad,
                                      Verkaufseinheit einheit, double steuerSatz) {
        Produkt produkt = new Produkt(0, name, preis, anfangsbestand, bildPfad, einheit, steuerSatz);
        Produkt saved = repository.speichern(produkt);
        notifyListeners();
        return saved;
    }

    public void warenzugangErfassen(int produktId, int menge) {
        warenzugangErfassen(produktId, (double) menge);
    }

    /**
     * Bucht einen Warenzugang und erhoeht dadurch den Lagerbestand.
     *
     * @param produktId Produktnummer.
     * @param menge Zugegangene Menge.
     */
    public void warenzugangErfassen(int produktId, double menge) {
        Produkt produkt = repository.findeNachId(produktId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
        produkt.bestandErhoehen(menge);
        repository.speichern(produkt);
        notifyListeners();
    }

    public List<Produkt> alleProdukte() {
        return repository.findeAlle();
    }

    public void produktLoeschen(int produktId) {
        repository.findeNachId(produktId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
        repository.loeschen(produktId);
        notifyListeners();
    }

    public Produkt produktAktualisieren(int produktId, String name, double preis, int lagerbestand) {
        return produktAktualisieren(produktId, name, preis, lagerbestand, null);
    }

    public Produkt produktAktualisieren(int produktId, String name, double preis, int lagerbestand, String bildPfad) {
        return produktAktualisieren(produktId, name, preis, (double) lagerbestand, bildPfad);
    }

    public Produkt produktAktualisieren(int produktId, String name, double preis, double lagerbestand, String bildPfad) {
        return produktAktualisieren(produktId, name, preis, lagerbestand, bildPfad,
                Verkaufseinheit.STUECK, Steuersatz.REGELSTEUERSATZ.getProzent());
    }

    /**
     * Aktualisiert ein vorhandenes Produkt.
     *
     * @param produktId Produktnummer.
     * @param name Neuer Produktname.
     * @param preis Neuer Bruttopreis.
     * @param lagerbestand Neuer Lagerbestand.
     * @param bildPfad Optional neuer Bildpfad.
     * @param einheit Neue Verkaufseinheit.
     * @param steuerSatz Neuer Umsatzsteuersatz.
     * @return Gespeichertes Produkt nach der Aktualisierung.
     */
    public Produkt produktAktualisieren(int produktId, String name, double preis, double lagerbestand, String bildPfad,
                                        Verkaufseinheit einheit, double steuerSatz) {
        Produkt produkt = repository.findeNachId(produktId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
        produkt.setName(name);
        produkt.setPreis(preis);
        produkt.setLagerbestand(lagerbestand);
        produkt.setEinheit(einheit);
        produkt.setSteuerSatz(steuerSatz);
        if (bildPfad != null) {
            produkt.setBildPfad(bildPfad);
        }
        Produkt updated = repository.speichern(produkt);
        notifyListeners();
        return updated;
    }

    public boolean istMengeVerfuegbar(int produktId, int menge) {
        return istMengeVerfuegbar(produktId, (double) menge);
    }

    /**
     * Prueft, ob eine Menge fuer den Verkauf verfuegbar ist.
     *
     * @param produktId Produktnummer.
     * @param menge Gewuenschte Menge.
     * @return {@code true}, wenn das Produkt existiert und genug Bestand vorhanden ist.
     */
    public boolean istMengeVerfuegbar(int produktId, double menge) {
        return repository.findeNachId(produktId)
                .map(produkt -> produkt.getLagerbestand() >= menge)
                .orElse(false);
    }
}
