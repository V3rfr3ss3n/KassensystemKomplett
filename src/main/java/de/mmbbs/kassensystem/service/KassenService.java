package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.repository.BonHistorieRepository;
import de.mmbbs.kassensystem.repository.JsonBonHistorieRepository;
import de.mmbbs.kassensystem.repository.ProduktRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KassenService {
    private final ProduktRepository repository;
    private final BonHistorieRepository bonHistorieRepository;
    private final BonService bonService;
    private final List<BonPosition> warenkorb = new ArrayList<>();
    private final List<Bon> bonHistorie = new ArrayList<>();

    public KassenService(ProduktRepository repository) {
        this(repository, new JsonBonHistorieRepository());
    }

    public KassenService(ProduktRepository repository, BonHistorieRepository bonHistorieRepository) {
        this.repository = repository;
        this.bonHistorieRepository = bonHistorieRepository;
        this.bonService = new BonService();
        this.bonHistorie.addAll(bonHistorieRepository.ladeBonHistorie());
    }

    public void positionHinzufuegen(int produktId, int menge) {
        Produkt produkt = repository.findeNachId(produktId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
        int mengeImWarenkorb = warenkorb.stream()
                .filter(position -> position.getProdukt().getId() == produktId)
                .mapToInt(BonPosition::getMenge)
                .sum();
        if (produkt.getLagerbestand() < mengeImWarenkorb + menge) {
            throw new IllegalArgumentException("Nicht genügend Produkte auf Lager.");
        }
        warenkorb.add(new BonPosition(produkt, menge));
    }

    public List<BonPosition> getWarenkorb() {
        return List.copyOf(warenkorb);
    }

    public double berechneGesamtpreis() {
        return warenkorb.stream().mapToDouble(BonPosition::getGesamtpreis).sum();
    }

    public Bon kassenvorgangAbschliessen() {
        if (warenkorb.isEmpty()) {
            throw new IllegalArgumentException("Der Warenkorb ist leer.");
        }

        Map<Integer, Integer> benoetigteMengen = new LinkedHashMap<>();
        for (BonPosition position : warenkorb) {
            int produktId = position.getProdukt().getId();
            benoetigteMengen.merge(produktId, position.getMenge(), Integer::sum);
        }

        for (Map.Entry<Integer, Integer> eintrag : benoetigteMengen.entrySet()) {
            Produkt produkt = repository.findeNachId(eintrag.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
            if (produkt.getLagerbestand() < eintrag.getValue()) {
                throw new IllegalArgumentException("Nicht genügend Produkte auf Lager.");
            }
        }

        for (BonPosition position : warenkorb) {
            Produkt produkt = repository.findeNachId(position.getProdukt().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
            produkt.bestandVerringern(position.getMenge());
            repository.speichern(produkt);
        }

        Bon bon = bonService.erstelleBon(new ArrayList<>(warenkorb));
        bonHistorie.add(bon);
        bonHistorieRepository.speichereBonHistorie(new ArrayList<>(bonHistorie));
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
