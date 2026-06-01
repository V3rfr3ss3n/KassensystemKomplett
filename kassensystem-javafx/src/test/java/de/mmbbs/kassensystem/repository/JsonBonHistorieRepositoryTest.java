package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonBonHistorieRepositoryTest {

    @Test
    void ladeBonHistorie_gibtLeereListeBeiBeschädigterDateiZurück() throws Exception {
        Path datei = Files.createTempFile("bon-historie-invalid-", ".json");
        Files.writeString(datei, "[");

        JsonBonHistorieRepository repository = new JsonBonHistorieRepository(datei);

        assertTrue(repository.ladeBonHistorie().isEmpty());
    }

    @Test
    void speichereUndLadeBonHistorie_rundTrip() throws Exception {
        Path datei = Files.createTempFile("bon-historie-roundtrip-", ".json");
        JsonBonHistorieRepository repository = new JsonBonHistorieRepository(datei);
        Produkt produkt = new Produkt(1, "Testprodukt", 2.50, 5);
        Bon bon = new Bon(7, List.of(new BonPosition(produkt, 2)));

        repository.speichereBonHistorie(List.of(bon));
        List<Bon> geladen = repository.ladeBonHistorie();

        assertEquals(1, geladen.size());
        assertEquals(7, geladen.get(0).getBonnummer());
        assertEquals(1, geladen.get(0).getPositionen().size());
        assertEquals("Testprodukt", geladen.get(0).getPositionen().get(0).getProdukt().getName());
    }
}
