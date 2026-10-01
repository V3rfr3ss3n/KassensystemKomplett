package de.mmbbs.kassensystem.backend.model;

import java.util.List;

public record KaufRequest(List<Position> positionen) {
    public record Position(int produktId, double menge) {}
}
