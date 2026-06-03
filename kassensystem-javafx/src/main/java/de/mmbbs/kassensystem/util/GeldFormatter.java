package de.mmbbs.kassensystem.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class GeldFormatter {
    private static final Locale DEUTSCHLAND = Locale.GERMANY;

    private GeldFormatter() {
    }

    public static String formatiereBetrag(double betrag) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(DEUTSCHLAND);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(betrag).replace('\u00A0', ' ');
    }

    public static String formatiereZahl(double wert) {
        NumberFormat formatter = NumberFormat.getNumberInstance(DEUTSCHLAND);
        formatter.setGroupingUsed(false);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(wert);
    }
}
