package de.mmbbs.kassensystem.repository;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonBonHistorieRepository implements BonHistorieRepository {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final Path dateipfad;

    public JsonBonHistorieRepository() {
        this(Path.of("bon-historie.json"));
    }

    public JsonBonHistorieRepository(Path dateipfad) {
        this.dateipfad = dateipfad;
    }

    @Override
    public List<Bon> ladeBonHistorie() {
        if (!Files.exists(dateipfad)) {
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(dateipfad, StandardCharsets.UTF_8)) {
            BonJson[] bons = objectMapper.readValue(reader, BonJson[].class);
            if (bons == null) {
                return new ArrayList<>();
            }
            return Arrays.stream(bons)
                    .map(BonJson::toBon)
                    .toList();
        } catch (IOException ex) {
            return new ArrayList<>();
        }
    }

    @Override
    public void speichereBonHistorie(List<Bon> bonHistorie) {
        try {
            Path parent = dateipfad.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(dateipfad, StandardCharsets.UTF_8)) {
                objectMapper.writeValue(writer, bonHistorie.stream().map(BonJson::new).toList());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Bon-Historie konnte nicht gespeichert werden.", ex);
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class BonJson {
        private int bonnummer;
        private LocalDateTime datumUhrzeit;
        private List<BonPositionJson> positionen;
        private double gesamtpreis;

        private BonJson() {
        }

        private BonJson(Bon bon) {
            this.bonnummer = bon.getBonnummer();
            this.datumUhrzeit = bon.getDatumUhrzeit();
            this.positionen = bon.getPositionen().stream().map(BonPositionJson::new).toList();
            this.gesamtpreis = bon.getGesamtpreis();
        }

        private Bon toBon() {
            return new Bon(
                    bonnummer,
                    datumUhrzeit,
                    positionen.stream().map(BonPositionJson::toBonPosition).toList(),
                    gesamtpreis
            );
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class BonPositionJson {
        private ProduktJson produkt;
        private int menge;
        private double einzelpreis;
        private double gesamtpreis;

        private BonPositionJson() {
        }

        private BonPositionJson(BonPosition position) {
            this.produkt = new ProduktJson(position.getProdukt());
            this.menge = position.getMenge();
            this.einzelpreis = position.getEinzelpreis();
            this.gesamtpreis = position.getGesamtpreis();
        }

        private BonPosition toBonPosition() {
            return new BonPosition(produkt.toProdukt(), menge);
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static final class ProduktJson {
        private int id;
        private String name;
        private double preis;
        private int lagerbestand;

        private ProduktJson() {
        }

        private ProduktJson(Produkt produkt) {
            this.id = produkt.getId();
            this.name = produkt.getName();
            this.preis = produkt.getPreis();
            this.lagerbestand = produkt.getLagerbestand();
        }

        private Produkt toProdukt() {
            return new Produkt(id, name, preis, lagerbestand);
        }
    }
}
