package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.model.Produkt;

import java.util.List;
import java.util.Optional;

/**
 * Abstraktion fuer die Produktdatenhaltung.
 *
 * <p>Services arbeiten gegen dieses Interface und sind dadurch unabhaengig
 * davon, ob Produkte im Speicher, in JSON oder in SQLite gespeichert werden.</p>
 */
public interface ProduktRepository {
    /**
     * Laedt alle Produkte.
     */
    List<Produkt> findeAlle();

    /**
     * Sucht ein Produkt ueber seine Produktnummer.
     */
    Optional<Produkt> findeNachId(int id);

    /**
     * Speichert ein neues oder geaendertes Produkt.
     */
    Produkt speichern(Produkt produkt);

    /**
     * Entfernt ein Produkt aus der Datenhaltung.
     */
    void loeschen(int id);
}
