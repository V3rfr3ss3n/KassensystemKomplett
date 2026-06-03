package de.mmbbs.kassensystem.model;

import java.util.Arrays;

public enum Verkaufseinheit {
    STUECK("Stück"),
    KILOGRAMM("kg"),
    LITER("l"),
    PACKUNG("Packung");

    private final String label;

    Verkaufseinheit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Verkaufseinheit fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return STUECK;
        }
        return Arrays.stream(values())
                .filter(einheit -> einheit.label.equalsIgnoreCase(label) || einheit.name().equalsIgnoreCase(label))
                .findFirst()
                .orElse(STUECK);
    }

    @Override
    public String toString() {
        return label;
    }
}
