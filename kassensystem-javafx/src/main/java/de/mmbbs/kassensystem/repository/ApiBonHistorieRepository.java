package de.mmbbs.kassensystem.repository;

import com.fasterxml.jackson.databind.JsonNode;
import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Verkaufseinheit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiBonHistorieRepository implements BonHistorieRepository, RemoteKaufRepository {
    private final ApiClient api;

    public ApiBonHistorieRepository(ApiClient api) { this.api = api; }

    @Override public List<Bon> ladeBonHistorie() {
        List<Bon> bons = new ArrayList<>();
        for (JsonNode node : api.get("/api/bons")) bons.add(lese(node));
        return bons;
    }

    @Override public void speichereBonHistorie(List<Bon> bonHistorie) {
        throw new UnsupportedOperationException("Bons werden nur ueber den Kaufabschluss gespeichert.");
    }

    @Override public Bon schliesseKaufAb(Map<Integer, Double> mengen) {
        List<Map<String, Object>> positionen = mengen.entrySet().stream()
                .map(e -> Map.<String, Object>of("produktId", e.getKey(), "menge", e.getValue())).toList();
        return lese(api.post("/api/kasse/abschluss", Map.of("positionen", positionen)));
    }

    private static Bon lese(JsonNode node) {
        List<BonPosition> positionen = new ArrayList<>();
        for (JsonNode item : node.path("positionen")) {
            double preis = item.path("einzelpreis").asDouble();
            double steuer = item.path("steuerSatz").asDouble(19);
            Produkt produkt = new Produkt(item.path("produktId").asInt(), item.path("produktName").asText(),
                    preis, 0.0, null, Verkaufseinheit.valueOf(item.path("einheit").asText("STUECK")), steuer);
            positionen.add(new BonPosition(produkt, item.path("menge").asDouble(), preis,
                    item.path("gesamtpreis").asDouble(), steuer));
        }
        return new Bon(node.path("bonnummer").asInt(), LocalDateTime.parse(node.path("datumUhrzeit").asText()),
                positionen, node.path("gesamtpreis").asDouble());
    }
}
