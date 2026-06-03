package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.repository.BonHistorieRepository;
import de.mmbbs.kassensystem.repository.InMemoryProduktRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KassenServiceTest {

    @Test
    void kassenvorgangAbschliessen_reduziertLagerbestandUndErzeugtBon() {
        InMemoryProduktRepository repository = new InMemoryProduktRepository();
        TestBonHistorieRepository historie = new TestBonHistorieRepository();
        KassenService service = new KassenService(repository, historie);

        service.positionHinzufuegen(1, 2);

        Bon bon = service.kassenvorgangAbschliessen();

        assertNotNull(bon);
        assertEquals(18, repository.findeNachId(1).orElseThrow().getLagerbestand());
        assertEquals(1, historie.saved.size());
        assertTrue(service.getWarenkorb().isEmpty());
    }

    @Test
    void warenkorbPositionenKoennenBearbeitetWerden() {
        InMemoryProduktRepository repository = new InMemoryProduktRepository();
        KassenService service = new KassenService(repository, new TestBonHistorieRepository());

        service.positionHinzufuegen(1, 2);
        BonPosition position = service.getWarenkorb().get(0);

        service.positionErhoehen(position);
        assertEquals(1, service.getWarenkorb().size());
        assertEquals(3.0, service.getWarenkorb().get(0).getMenge(), 0.001);

        service.positionVerringern(service.getWarenkorb().get(0));
        assertEquals(2.0, service.getWarenkorb().get(0).getMenge(), 0.001);

        service.positionEntfernen(service.getWarenkorb().get(0));
        assertTrue(service.getWarenkorb().isEmpty());
    }

    @Test
    void gleichesProduktWirdImWarenkorbZusammengefuehrt() {
        InMemoryProduktRepository repository = new InMemoryProduktRepository();
        KassenService service = new KassenService(repository, new TestBonHistorieRepository());

        service.positionHinzufuegen(1, 1);
        service.positionHinzufuegen(1, 2);

        assertEquals(1, service.getWarenkorb().size());
        assertEquals(3.0, service.getWarenkorb().get(0).getMenge(), 0.001);
    }

    private static final class TestBonHistorieRepository implements BonHistorieRepository {
        private final List<Bon> saved = new ArrayList<>();

        @Override
        public List<Bon> ladeBonHistorie() {
            return new ArrayList<>();
        }

        @Override
        public void speichereBonHistorie(List<Bon> bonHistorie) {
            saved.addAll(bonHistorie);
        }
    }
}
