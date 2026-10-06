package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.model.Produkt;

import java.util.List;
import java.util.Optional;

/**
 * Abstraktion fuer die Produktdatenhaltung.
 *
 * <p>Die Kasse liest Produktdaten ausschließlich über die Backend-API.</p>
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
}
