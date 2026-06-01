package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.repository.ProduktRepository;

import java.util.ArrayList;
import java.util.List;

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
        Produkt produkt = new Produkt(0, name, preis, anfangsbestand, bildPfad);
        Produkt saved = repository.speichern(produkt);
        notifyListeners();
        return saved;
    }

    public void warenzugangErfassen(int produktId, int menge) {
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
        Produkt produkt = repository.findeNachId(produktId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
        produkt.setName(name);
        produkt.setPreis(preis);
        produkt.setLagerbestand(lagerbestand);
        if (bildPfad != null) {
            produkt.setBildPfad(bildPfad);
        }
        Produkt updated = repository.speichern(produkt);
        notifyListeners();
        return updated;
    }

    public boolean istMengeVerfuegbar(int produktId, int menge) {
        return repository.findeNachId(produktId)
                .map(produkt -> produkt.getLagerbestand() >= menge)
                .orElse(false);
    }
}
