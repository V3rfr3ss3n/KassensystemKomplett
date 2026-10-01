package de.mmbbs.kassensystem.auth;

import java.util.Map;
import java.util.Optional;

/**
 * Einfache Testnutzer-Anmeldung fuer die JavaFX-Anwendung.
 *
 * <p>Die Nutzer sind bewusst fest im Code hinterlegt, weil es sich aktuell um
 * ein Schulprojekt mit Testzugang handelt. Spaeter kann diese Klasse durch eine
 * Datenbank- oder Serveranmeldung ersetzt werden.</p>
 */
public class AuthService {
    private final Map<String, TestNutzer> nutzer = Map.of(
            "admin", new TestNutzer("1234", BenutzerRolle.ADMIN),
            "kassierer", new TestNutzer("1234", BenutzerRolle.KASSIERER),
            "lagerist", new TestNutzer("1234", BenutzerRolle.LAGERIST)
    );

    /**
     * Prueft Benutzername und Passwort.
     *
     * @return Angemeldeter Benutzer oder leer, wenn die Daten falsch sind.
     */
    public Optional<Benutzer> anmelden(String benutzername, String passwort) {
        if (benutzername == null || passwort == null) {
            return Optional.empty();
        }
        String login = benutzername.trim().toLowerCase();
        TestNutzer testNutzer = nutzer.get(login);
        if (testNutzer == null || !testNutzer.passwort().equals(passwort)) {
            return Optional.empty();
        }
        return Optional.of(new Benutzer(login, testNutzer.rolle()));
    }

    private record TestNutzer(String passwort, BenutzerRolle rolle) {
    }
}
