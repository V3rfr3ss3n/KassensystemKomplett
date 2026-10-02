package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Steuersatz;
import de.mmbbs.kassensystem.model.Verkaufseinheit;
import de.mmbbs.kassensystem.repository.InMemoryProduktRepository;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProduktServiceTest {

    @Test
    void produktpflegeUndWarenzugangBenachrichtigenAnsichten() {
        InMemoryProduktRepository repository = new InMemoryProduktRepository();
        ProduktService service = new ProduktService(repository);
        AtomicInteger meldungen = new AtomicInteger();
        ProductChangeListener listener = meldungen::incrementAndGet;
        service.addListener(listener);

        Produkt produkt = service.produktHinzufuegen("  Birne  ", 1.25, 3.5, null,
                Verkaufseinheit.KILOGRAMM, Steuersatz.ERMAESSIGT.getProzent());
        assertEquals("Birne", produkt.getName());
        assertEquals(1, meldungen.get());
        assertEquals(6, service.alleProdukte().size());

        service.warenzugangErfassen(produkt.getId(), 2.5);
        assertEquals(6.0, repository.findeNachId(produkt.getId()).orElseThrow().getLagerbestand());
        assertTrue(service.istMengeVerfuegbar(produkt.getId(), 6));
        assertFalse(service.istMengeVerfuegbar(produkt.getId(), 7));
        assertFalse(service.istMengeVerfuegbar(9999, 1));

        Produkt aktualisiert = service.produktAktualisieren(produkt.getId(), "Apfel", 2.0, 4.0,
                "apfel.png", Verkaufseinheit.STUECK, Steuersatz.REGELSTEUERSATZ.getProzent());
        assertEquals("Apfel", aktualisiert.getName());
        assertEquals("apfel.png", aktualisiert.getBildPfad());
        assertEquals(4.0, aktualisiert.getLagerbestand());
        assertEquals(3, meldungen.get());

        service.produktLoeschen(produkt.getId());
        assertEquals(4, meldungen.get());
        assertEquals(5, service.alleProdukte().size());
        service.removeListener(listener);
        service.produktHinzufuegen("Orange", 1.0, 1);
        assertEquals(4, meldungen.get());
    }

    @Test
    void ungueltigeVorgaengeSpeichernNichtsUndMeldenKeineAenderung() {
        InMemoryProduktRepository repository = new InMemoryProduktRepository();
        ProduktService service = new ProduktService(repository);
        AtomicInteger meldungen = new AtomicInteger();
        service.addListener(meldungen::incrementAndGet);

        assertThrows(IllegalArgumentException.class, () -> service.produktHinzufuegen("", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> service.produktHinzufuegen("Test", Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> service.warenzugangErfassen(1, -1));
        assertThrows(IllegalArgumentException.class, () -> service.warenzugangErfassen(9999, 1));
        assertThrows(IllegalArgumentException.class, () -> service.produktLoeschen(9999));
        assertThrows(IllegalArgumentException.class,
                () -> service.produktAktualisieren(9999, "Test", 1, 1));
        assertEquals(5, service.alleProdukte().size());
        assertEquals(0, meldungen.get());
    }
}
