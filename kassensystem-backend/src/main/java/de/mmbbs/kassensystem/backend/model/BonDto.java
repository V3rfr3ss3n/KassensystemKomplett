package de.mmbbs.kassensystem.backend.model;

import java.time.LocalDateTime;
import java.util.List;

public record BonDto(int bonnummer, LocalDateTime datumUhrzeit, double gesamtpreis,
                     List<Position> positionen) {
    public record Position(int produktId, String produktName, String einheit, double menge,
                           double einzelpreis, double steuerSatz, double gesamtpreis) {}
}
