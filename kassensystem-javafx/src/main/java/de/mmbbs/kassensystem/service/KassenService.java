package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.repository.KaufRepository;
import de.mmbbs.kassensystem.repository.ProduktRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fachlogik fuer Kassenvorgaenge.
 *
 * <p>Der Service verwaltet den Warenkorb und prüft verfügbare Bestände.
 * Das Backend speichert Bon und Lagerabbuchung gemeinsam.</p>
 */
public class KassenService {
    private final ProduktRepository repository;
    private final KaufRepository kaufRepository;
    private final List<BonPosition> warenkorb = new ArrayList<>();
    private final List<Bon> bonHistorie = new ArrayList<>();

    public KassenService(ProduktRepository repository, KaufRepository kaufRepository) {
        this.repository = repository;
        this.kaufRepository = kaufRepository;
        this.bonHistorie.addAll(kaufRepository.ladeBonHistorie());
    }

    public void positionHinzufuegen(int produktId, int menge) {
        positionHinzufuegen(produktId, (double) menge);
    }

    /**
     * Fuegt ein Produkt in den Warenkorb ein oder erhoeht eine vorhandene Position.
     *
     * @param produktId Produktnummer des gewaehlten Produkts.
     * @param menge Gewaehlte Menge.
     */
    public void positionHinzufuegen(int produktId, double menge) {
        if (!Double.isFinite(menge) || menge <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        Produkt produkt = repository.findeNachId(produktId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
        double bisherigeMenge = warenkorb.stream()
                .filter(position -> position.getProdukt().getId() == produktId)
                .mapToDouble(BonPosition::getMenge)
                .sum();
        double neueMenge = bisherigeMenge + menge;
        if (produkt.getLagerbestand() < neueMenge) {
            throw new IllegalArgumentException("Nicht genügend Produkte auf Lager.");
        }

        int erstePosition = -1;
        for (int i = 0; i < warenkorb.size(); i++) {
            if (warenkorb.get(i).getProdukt().getId() == produktId) {
                if (erstePosition < 0) {
                    erstePosition = i;
                } else {
                    warenkorb.remove(i);
                    i--;
                }
            }
        }

        BonPosition neuePosition = new BonPosition(produkt, neueMenge);
        if (erstePosition >= 0) {
            warenkorb.set(erstePosition, neuePosition);
        } else {
            warenkorb.add(neuePosition);
        }
    }

    public List<BonPosition> getWarenkorb() {
        return List.copyOf(warenkorb);
    }

    /**
     * Entfernt eine einzelne Warenkorbposition.
     */
    public void positionEntfernen(BonPosition position) {
        warenkorb.remove(position);
    }

    /**
     * Erhoeht eine Warenkorbposition um eine Einheit, wenn genug Bestand vorhanden ist.
     */
    public void positionErhoehen(BonPosition position) {
        int index = warenkorb.indexOf(position);
        if (index < 0) {
            return;
        }

        Produkt produkt = repository.findeNachId(position.getProdukt().getId())
                .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
        double mengeImWarenkorb = warenkorb.stream()
                .filter(eintrag -> eintrag.getProdukt().getId() == position.getProdukt().getId())
                .mapToDouble(BonPosition::getMenge)
                .sum();
        if (produkt.getLagerbestand() < mengeImWarenkorb + 1.0) {
            throw new IllegalArgumentException("Nicht genügend Produkte auf Lager.");
        }

        warenkorb.set(index, new BonPosition(position.getProdukt(), position.getMenge() + 1.0,
                position.getEinzelpreis(), Double.NaN, position.getSteuerSatz()));
    }

    /**
     * Verringert eine Warenkorbposition um eine Einheit oder entfernt sie bei Menge 0.
     */
    public void positionVerringern(BonPosition position) {
        int index = warenkorb.indexOf(position);
        if (index < 0) {
            return;
        }

        double neueMenge = position.getMenge() - 1.0;
        if (neueMenge <= 0) {
            warenkorb.remove(index);
            return;
        }

        warenkorb.set(index, new BonPosition(position.getProdukt(), neueMenge,
                position.getEinzelpreis(), Double.NaN, position.getSteuerSatz()));
    }

    /**
     * Berechnet den aktuellen Warenkorbwert.
     */
    public double berechneGesamtpreis() {
        return warenkorb.stream().mapToDouble(BonPosition::getGesamtpreis).sum();
    }

    /**
     * Schliesst den Kassenvorgang ab.
     *
     * <p>Die Bestände werden vorab geprüft. Das Backend erzeugt Bon und
     * Lagerabbuchung in einer Transaktion; erst nach Erfolg wird der Warenkorb geleert.</p>
     *
     * @return Erzeugter Bon des abgeschlossenen Kaufs.
     */
    public Bon kassenvorgangAbschliessen() {
        if (warenkorb.isEmpty()) {
            throw new IllegalArgumentException("Der Warenkorb ist leer.");
        }

        Map<Integer, Double> benoetigteMengen = new LinkedHashMap<>();
        for (BonPosition position : warenkorb) {
            int produktId = position.getProdukt().getId();
            benoetigteMengen.merge(produktId, position.getMenge(), Double::sum);
        }

        for (Map.Entry<Integer, Double> eintrag : benoetigteMengen.entrySet()) {
            Produkt produkt = repository.findeNachId(eintrag.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
            if (produkt.getLagerbestand() < eintrag.getValue()) {
                throw new IllegalArgumentException("Nicht genügend Produkte auf Lager.");
            }
        }

        Bon bon = kaufRepository.schliesseKaufAb(benoetigteMengen);
        bonHistorie.add(0, bon);
        warenkorb.clear();
        return bon;
    }

    public List<Bon> getBonHistorie() {
        return List.copyOf(bonHistorie);
    }

    public void warenkorbLeeren() {
        warenkorb.clear();
    }
}
