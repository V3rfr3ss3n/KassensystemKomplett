package de.mmbbs.kassensystem.backend.model;

public record ProduktRequest(
        String name,
        Double preis,
        Double lagerbestand,
        String bildPfad,
        String einheit,
        Double steuerSatz,
        String kategorie,
        String scanCode
) {
}
