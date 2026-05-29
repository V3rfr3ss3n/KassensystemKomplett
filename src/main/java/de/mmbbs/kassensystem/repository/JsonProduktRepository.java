package de.mmbbs.kassensystem.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import de.mmbbs.kassensystem.model.Produkt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonProduktRepository implements ProduktRepository {
    private final Map<Integer, Produkt> produkte = new LinkedHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dateipfad;
    private int nextId = 1;

    public JsonProduktRepository() {
        this(Path.of("produkte.json"));
    }

    public JsonProduktRepository(Path dateipfad) {
        this.dateipfad = dateipfad;
        ladeOderErzeugeInitialeDaten();
    }

    @Override
    public List<Produkt> findeAlle() {
        return new ArrayList<>(produkte.values());
    }

    @Override
    public Optional<Produkt> findeNachId(int id) {
        return Optional.ofNullable(produkte.get(id));
    }

    @Override
    public Produkt speichern(Produkt produkt) {
        Produkt zuSpeichern = produkt;
        if (produkt.getId() == 0) {
            zuSpeichern = new Produkt(nextId(), produkt.getName(), produkt.getPreis(), produkt.getLagerbestand());
        }

        produkte.put(zuSpeichern.getId(), zuSpeichern);
        speichereDatei();
        return zuSpeichern;
    }

    @Override
    public void loeschen(int id) {
        produkte.remove(id);
        speichereDatei();
    }

    private void ladeOderErzeugeInitialeDaten() {
        if (Files.exists(dateipfad)) {
            ladeDatei();
            return;
        }

        speichereInitialeDaten();
        speichereDatei();
    }

    private void ladeDatei() {
        try (Reader reader = Files.newBufferedReader(dateipfad, StandardCharsets.UTF_8)) {
            ProduktJson[] produktDaten = gson.fromJson(reader, ProduktJson[].class);
            if (produktDaten == null) {
                return;
            }

            for (ProduktJson produktJson : produktDaten) {
                Produkt produkt = produktJson.toProdukt();
                produkte.put(produkt.getId(), produkt);
                nextId = Math.max(nextId, produkt.getId() + 1);
            }
        } catch (IOException | JsonSyntaxException ex) {
            throw new IllegalStateException("Produktdaten konnten nicht geladen werden.", ex);
        }
    }

    private void speichereInitialeDaten() {
        speichereOhneDatei(new Produkt(nextId(), "Cola", 1.50, 20));
        speichereOhneDatei(new Produkt(nextId(), "Wasser", 1.00, 30));
        speichereOhneDatei(new Produkt(nextId(), "Br\u00f6tchen", 0.80, 15));
        speichereOhneDatei(new Produkt(nextId(), "Kaffee", 2.20, 10));
        speichereOhneDatei(new Produkt(nextId(), "Schokoriegel", 1.20, 25));
    }

    private void speichereOhneDatei(Produkt produkt) {
        produkte.put(produkt.getId(), produkt);
    }

    private void speichereDatei() {
        try {
            Path parent = dateipfad.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            List<ProduktJson> produktDaten = produkte.values().stream()
                    .map(ProduktJson::new)
                    .toList();
            try (Writer writer = Files.newBufferedWriter(dateipfad, StandardCharsets.UTF_8)) {
                gson.toJson(produktDaten, writer);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Produktdaten konnten nicht gespeichert werden.", ex);
        }
    }

    private int nextId() {
        return nextId++;
    }

    private static final class ProduktJson {
        private int id;
        private String name;
        private double preis;
        private int lagerbestand;
        private String bildPfad;

        private ProduktJson() {
        }

        private ProduktJson(Produkt produkt) {
            this.id = produkt.getId();
            this.name = produkt.getName();
            this.preis = produkt.getPreis();
            this.lagerbestand = produkt.getLagerbestand();
            this.bildPfad = produkt.getBildPfad();
        }

        private Produkt toProdukt() {
            return new Produkt(id, name, preis, lagerbestand, bildPfad);
        }
    }
}
