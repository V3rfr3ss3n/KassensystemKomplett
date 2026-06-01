package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.model.Produkt;

import java.util.List;
import java.util.Optional;

public interface ProduktRepository {
    List<Produkt> findeAlle();

    Optional<Produkt> findeNachId(int id);

    Produkt speichern(Produkt produkt);

    void loeschen(int id);
}
