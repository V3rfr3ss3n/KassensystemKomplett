package de.mmbbs.kassensystem.auth;

/**
 * Angemeldeter Testbenutzer der JavaFX-Anwendung.
 *
 * @param benutzername Anzeigename und Loginname.
 * @param rolle Berechtigungsrolle des Benutzers.
 */
public record Benutzer(String benutzername, BenutzerRolle rolle) {
    public boolean istAdmin() {
        return rolle == BenutzerRolle.ADMIN;
    }

    public boolean istLagerist() {
        return rolle == BenutzerRolle.LAGERIST;
    }

    public boolean darfKassieren() {
        return rolle == BenutzerRolle.ADMIN || rolle == BenutzerRolle.KASSIERER;
    }

    public boolean darfProduktverwaltung() {
        return rolle == BenutzerRolle.ADMIN;
    }

    public boolean darfWarenzugangBuchen() {
        return rolle == BenutzerRolle.ADMIN || rolle == BenutzerRolle.LAGERIST;
    }

    public boolean darfWebVerwaltungNutzen() {
        return darfProduktverwaltung() || darfWarenzugangBuchen();
    }
}
