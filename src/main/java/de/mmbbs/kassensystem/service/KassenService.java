package de.mmbbs.kassensystem.service;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.repository.ProduktRepository;

import java.util.ArrayList;
import java.util.List;

public class KassenService {
    private final ProduktRepository repository;
    private final BonService bonService;
    private final List<BonPosition> warenkorb = new ArrayList<>();
    private final List<Bon> bonHistorie = new ArrayList<>();

    public KassenService(ProduktRepository repository) {
        this.repository = repository;
        this.bonService = new BonService();
    }

    public void positionHinzufuegen(int produktId, int menge) {
        Produkt produkt = repository.findeNachId(produktId)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
        if (produkt.getLagerbestand() < menge) {
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

        for (BonPosition position : warenkorb) {
            Produkt produkt = repository.findeNachId(position.getProdukt().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden."));
            produkt.bestandVerringern(position.getMenge());
            repository.speichern(produkt);
        }

        Bon bon = bonService.erstelleBon(new ArrayList<>(warenkorb));
        bonHistorie.add(bon);
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
