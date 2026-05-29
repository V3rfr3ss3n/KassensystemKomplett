package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.repository.ProduktRepository;

import java.util.List;

public class ProduktService {
    private final ProduktRepository repository;

    public ProduktService(ProduktRepository repository) {
        this.repository = repository;
    }

    public Produkt produktHinzufuegen(String name, double preis, int anfangsbestand) {
        return produktHinzufuegen(name, preis, anfangsbestand, null);
    }

    public Produkt produktHinzufuegen(String name, double preis, int anfangsbestand, String bildPfad) {
        Produkt produkt = new Produkt(0, name, preis, anfangsbestand, bildPfad);
        return repository.speichern(produkt);
    }

    public void warenzugangErfassen(int produktId, int menge) {
        Produkt produkt = repository.findeNachId(produktId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
        produkt.bestandErhoehen(menge);
        repository.speichern(produkt);
    }

    public List<Produkt> alleProdukte() {
        return repository.findeAlle();
    }

    public void produktLoeschen(int produktId) {
        repository.findeNachId(produktId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
        repository.loeschen(produktId);
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
        return repository.speichern(produkt);
    }

    public boolean istMengeVerfuegbar(int produktId, int menge) {
        return repository.findeNachId(produktId)
                .map(produkt -> produkt.getLagerbestand() >= menge)
                .orElse(false);
    }
}
