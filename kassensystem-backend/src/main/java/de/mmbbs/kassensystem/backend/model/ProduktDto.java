package de.mmbbs.kassensystem.backend.model;

public record ProduktDto(
        int id,
        String name,
        double preis,
        double lagerbestand,
        String bildPfad,
        String einheit,
        String einheitLabel,
        double steuerSatz,
        String kategorie
) {
}
