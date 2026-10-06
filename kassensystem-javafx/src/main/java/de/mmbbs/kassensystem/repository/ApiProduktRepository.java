package de.mmbbs.kassensystem.repository;

import com.fasterxml.jackson.databind.JsonNode;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Verkaufseinheit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApiProduktRepository implements ProduktRepository {
    private final ApiClient api;

    public ApiProduktRepository(ApiClient api) { this.api = api; }

    @Override public List<Produkt> findeAlle() {
        List<Produkt> produkte = new ArrayList<>();
        for (JsonNode item : api.get("/api/produkte")) produkte.add(lese(item));
        return produkte;
    }

    @Override public Optional<Produkt> findeNachId(int id) {
        return findeAlle().stream().filter(p -> p.getId() == id).findFirst();
    }

    private static Produkt lese(JsonNode node) {
        Produkt produkt = new Produkt(node.path("id").asInt(), node.path("name").asText(), node.path("preis").asDouble(),
                node.path("lagerbestand").asDouble(), node.path("bildPfad").asText(null),
                Verkaufseinheit.valueOf(node.path("einheit").asText("STUECK")), node.path("steuerSatz").asDouble(19));
        produkt.setKategorie(node.path("kategorie").asText("Sonstiges"));
        produkt.setScanCode(node.path("scanCode").asText(null));
        return produkt;
    }
}
