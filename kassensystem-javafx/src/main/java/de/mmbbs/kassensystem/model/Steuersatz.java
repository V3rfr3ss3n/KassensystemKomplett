package de.mmbbs.kassensystem.model;

import java.util.Arrays;

public enum Steuersatz {
    REGELSTEUERSATZ(19.0, "Normal 19 %"),
    ERMAESSIGT(7.0, "Ermäßigt 7 %");

    private final double prozent;
    private final String label;

    Steuersatz(double prozent, String label) {
        this.prozent = prozent;
        this.label = label;
    }

    public double getProzent() {
        return prozent;
    }

    public static Steuersatz fromProzent(double prozent) {
        return Arrays.stream(values())
                .filter(steuersatz -> Math.abs(steuersatz.prozent - prozent) < 0.001)
                .findFirst()
                .orElse(REGELSTEUERSATZ);
    }

    @Override
    public String toString() {
        return label;
    }
}
