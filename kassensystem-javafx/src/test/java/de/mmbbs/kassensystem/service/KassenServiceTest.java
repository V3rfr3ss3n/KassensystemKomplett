package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.repository.KaufRepository;
import de.mmbbs.kassensystem.repository.ProduktRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KassenServiceTest {
    private final Produkt cola = new Produkt(1, "Cola", 1.50, 20);
    private final ProduktRepository produkte = new ProduktRepository() {
        public List<Produkt> findeAlle() { return List.of(cola); }
        public Optional<Produkt> findeNachId(int id) { return id == 1 ? Optional.of(cola) : Optional.empty(); }
    };
    private final TestKaufRepository kauf = new TestKaufRepository();

    @Test
    void kaufWirdEinmalAnBackendUebergebenUndWarenkorbErstDanachGeleert() {
        KassenService service = new KassenService(produkte, kauf);
        service.positionHinzufuegen(1, 1);
        service.positionHinzufuegen(1, 2);
        Bon bon = service.kassenvorgangAbschliessen();
        assertEquals(Map.of(1, 3.0), kauf.letzterKauf);
        assertEquals(1, bon.getBonnummer());
        assertTrue(service.getWarenkorb().isEmpty());
        assertEquals(1, service.getBonHistorie().size());
    }

    @Test
    void backendFehlerLaesstWarenkorbErhalten() {
        KaufRepository fehler = new KaufRepository() {
            public List<Bon> ladeBonHistorie() { return List.of(); }
            public Bon schliesseKaufAb(Map<Integer, Double> mengen) { throw new IllegalStateException("Backend offline"); }
        };
        KassenService service = new KassenService(produkte, fehler);
        service.positionHinzufuegen(1, 1);
        assertThrows(IllegalStateException.class, service::kassenvorgangAbschliessen);
        assertEquals(1, service.getWarenkorb().size());
    }

    @Test
    void warenkorbPositionenKoennenBearbeitetWerden() {
        KassenService service = new KassenService(produkte, kauf);
        service.positionHinzufuegen(1, 2);
        BonPosition position = service.getWarenkorb().get(0);
        service.positionErhoehen(position);
        assertEquals(3, service.getWarenkorb().get(0).getMenge());
        service.positionVerringern(service.getWarenkorb().get(0));
        assertEquals(2, service.getWarenkorb().get(0).getMenge());
        service.positionEntfernen(service.getWarenkorb().get(0));
        assertTrue(service.getWarenkorb().isEmpty());
    }

    @Test
    void wiederholtesHinzufuegenStopptAmLagerbestand() {
        KassenService service = new KassenService(produkte, kauf);
        service.positionHinzufuegen(1, 20);

        assertThrows(IllegalArgumentException.class, () -> service.positionHinzufuegen(1, 1));
        assertEquals(20, service.getWarenkorb().getFirst().getMenge());
    }

    private static final class TestKaufRepository implements KaufRepository {
        private Map<Integer, Double> letzterKauf;
        public List<Bon> ladeBonHistorie() { return List.of(); }
        public Bon schliesseKaufAb(Map<Integer, Double> mengen) {
            letzterKauf = Map.copyOf(mengen);
            return new Bon(1, List.of());
        }
    }
}
