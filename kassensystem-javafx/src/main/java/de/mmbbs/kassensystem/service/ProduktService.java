package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.repository.ProduktRepository;

import java.util.List;

/** Liest den aktuellen Produktkatalog des Backends für die Kasse. */
public class ProduktService {
    private final ProduktRepository repository;

    public ProduktService(ProduktRepository repository) {
        this.repository = repository;
    }

    public List<Produkt> alleProdukte() {
        return repository.findeAlle();
    }
}
