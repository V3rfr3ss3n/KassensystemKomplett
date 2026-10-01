package de.mmbbs.kassensystem.auth;

/**
 * Rollen fuer den lokalen JavaFX-Zugang.
 */
public enum BenutzerRolle {
    ADMIN("Admin"),
    KASSIERER("Kassierer"),
    LAGERIST("Lagerist");

    private final String anzeigename;

    BenutzerRolle(String anzeigename) {
        this.anzeigename = anzeigename;
    }

    public String getAnzeigename() {
        return anzeigename;
    }

    @Override
    public String toString() {
        return anzeigename;
    }
}
