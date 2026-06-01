package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.model.Produkt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryProduktRepository implements ProduktRepository {
    private final Map<Integer, Produkt> produkte = new LinkedHashMap<>();
    private int nextId = 1;

    public InMemoryProduktRepository() {
        speichereInitialeDaten();
    }

    private void speichereInitialeDaten() {
        speichere(new Produkt(nextId(), "Cola", 1.50, 20));
        speichere(new Produkt(nextId(), "Wasser", 1.00, 30));
        speichere(new Produkt(nextId(), "Brötchen", 0.80, 15));
        speichere(new Produkt(nextId(), "Kaffee", 2.20, 10));
        speichere(new Produkt(nextId(), "Schokoriegel", 1.20, 25));
    }

    private int nextId() {
        return nextId++;
    }

    private Produkt speichere(Produkt produkt) {
        produkte.put(produkt.getId(), produkt);
        return produkt;
    }

    @Override
    public List<Produkt> findeAlle() {
        return new ArrayList<>(produkte.values());
    }

    @Override
    public Optional<Produkt> findeNachId(int id) {
        return Optional.ofNullable(produkte.get(id));
    }

    @Override
    public Produkt speichern(Produkt produkt) {
        if (produkt.getId() == 0) {
            Produkt mitId = new Produkt(nextId(), produkt.getName(), produkt.getPreis(), produkt.getLagerbestand());
            return speichere(mitId);
        }
        return speichere(produkt);
    }

    @Override
    public void loeschen(int id) {
        produkte.remove(id);
    }
}
