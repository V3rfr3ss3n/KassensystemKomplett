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
}
