package de.mmbbs.kassensystem.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formatiert und liest Mengen im deutschen Zahlenformat.
 */
public final class MengenFormatter {
    private static final Locale DEUTSCHLAND = Locale.GERMANY;

    private MengenFormatter() {
    }

    public static String formatiereMenge(double menge) {
        NumberFormat formatter = NumberFormat.getNumberInstance(DEUTSCHLAND);
        formatter.setGroupingUsed(false);
        formatter.setMinimumFractionDigits(istGanzeZahl(menge) ? 0 : 1);
        formatter.setMaximumFractionDigits(3);
        return formatter.format(menge);
    }

    public static String formatiereMenge(double menge, String einheit) {
        return formatiereMenge(menge) + " " + einheit;
    }

    public static double parseMenge(String text) {
        return Double.parseDouble(text.trim().replace(",", "."));
    }

    private static boolean istGanzeZahl(double wert) {
        return Math.abs(wert - Math.rint(wert)) < 0.0001;
    }
}
