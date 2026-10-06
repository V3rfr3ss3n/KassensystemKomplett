# Kassensystem

Grafisches Kassensystem für einen kleinen Laden. Die JavaFX-App nutzt die REST-API des Spring-Boot-Backends für Produkte, Verkäufe und Bons. Nur das Backend greift auf SQLite zu.

## Schnellstart mit Docker

Unter Windows Docker Desktop installieren: `winget install -e --id Docker.DockerDesktop` (in UniGetUI nach `Docker.DockerDesktop` suchen). Docker Desktop starten. JDK 24 und Maven 3.9+ werden für den lokalen Build benötigt:

```powershell
mvn verify
$env:KASSENSYSTEM_AUTH_INITIAL_ADMIN_PASSWORD = 'EinEigenesStartpasswort123'
docker compose up --build -d
mvn -pl kassensystem-javafx javafx:run
```

Beim ersten Start wird damit `admin` angelegt. Dieses Startpasswort muss beim ersten Login geändert werden. Die Umgebungsvariable kann nach der Einrichtung entfernt werden. Für eine reine Entwicklungsdatenbank sind die alten Testkonten nur mit `KASSENSYSTEM_AUTH_DEMO=true` aktivierbar; niemals für eine normale Installation verwenden.

Die Verwaltung ist unter `http://127.0.0.1:8080/kassensystem/admin/` erreichbar. Die Daten liegen im Docker-Volume `kassensystem-data`. `docker compose down` stoppt das Backend und erhält die Daten. Die Kasse erreicht das Backend standardmäßig über `127.0.0.1:8080`; eine Hotspot-Verbindung ändert diese lokale Adresse nicht. Die Server-Wurzel `/` zeigt keine Seite.

Für ein anderes Backend `KASSENSYSTEM_API_URL` oder `-Dkassensystem.api.url=<URL>` setzen. Die URL enthält den Kontextpfad, etwa `http://localhost:8080/kassensystem`. Bei einer Release-ZIP die App mit `Kassensystem.exe` starten; Docker/Backend muss zuvor laufen.

## Downloads auf GitHub

Nach einem Tag wie `v1.0.0` veröffentlicht der Release-Workflow die ZIP `Kassensystem-Windows.zip` unter GitHub Releases und das Backend-Image als `ghcr.io/v3rfr3ss3n/kassensystemkomplett/backend:<tag>`. Ein Tag mit `-rc.` wird als Vorabversion markiert. Die ZIP enthält eine Java-Laufzeit und ein eigenes EXE-Symbol. Die ZIP vollständig in einen neuen Ordner entpacken und die dortige `Kassensystem.exe` starten; eine zuvor entpackte EXE wird durch einen neuen Download nicht automatisch ersetzt. Die Release-Version steht im Fenstertitel, damit sich die gestartete EXE zuordnen lässt. Vor dem ersten Release ist der Download als Artefakt eines manuell gestarteten Release-Workflows möglich. Container und Windows-Paket werden erst nach einem erfolgreichen Workflow-Lauf bereitgestellt.

**EXE und Backend müssen zusammenpassen.** Die EXE startet keinen Server. Ein älteres Backend kann zwar das frühere Demo-Passwort `1234` akzeptieren, liefert aber nicht die neuen Rechte für die Kasse. Für einen unabhängigen Test der Release-Version unter PowerShell zuerst einen bereits auf Port 8080 laufenden Server stoppen und dann das passende Backend-Image starten:

```powershell
$release = 'v0.1.0-rc.17'
$env:KASSENSYSTEM_AUTH_INITIAL_ADMIN_PASSWORD = 'EinEigenesStartpasswort123'
docker run --detach --rm --name kassensystem-test -p 127.0.0.1:8080:8080 `
  -e KASSENSYSTEM_DB_PATH=/data/kassensystem.db `
  -e KASSENSYSTEM_AUTH_INITIAL_ADMIN_PASSWORD `
  -v kassensystem-test-data:/data `
  "ghcr.io/v3rfr3ss3n/kassensystemkomplett/backend:$release"
```

Danach die EXE **derselben Version** starten, als `admin` mit dem selbst gewählten Startpasswort anmelden und es auf Aufforderung ändern. Die Testdaten bleiben im separaten Docker-Volume `kassensystem-test-data`. Nach dem Test `docker stop kassensystem-test` ausführen. `1234` gilt nur für ausdrücklich aktivierte Demo-Konten.

## Voraussetzungen und Start

- JDK **24**
- Apache Maven **3.9 oder neuer**
- Windows für das bereitgestellte Startskript; unter anderen Systemen die Maven-Befehle verwenden.

Unter Windows `start-kassensystem.cmd` im Projektordner ausführen. Das Skript prüft JDK und Maven, startet das Backend im Hintergrund und danach die JavaFX-Kasse. Die Startmeldungen des Backends stehen in `data/backend-start.log`. Dies ist die Entwicklung ohne Docker.
Vor dem ersten Start ein eigenes Startpasswort setzen: `$env:KASSENSYSTEM_AUTH_INITIAL_ADMIN_PASSWORD = 'EinEigenesStartpasswort123'`.

Alternativ in zwei Terminals aus dem Projektordner starten:

```powershell
mvn -pl kassensystem-backend spring-boot:run
mvn -pl kassensystem-javafx javafx:run
```

In einer IDE das Maven-Projekt am Wurzelordner öffnen, JDK 24 konfigurieren und die Klassen `KassensystemBackendApplication` und `Main` starten. Der Browser-Adminbereich liegt unter `http://localhost:8080/kassensystem/admin/`.

## Anmelden und bedienen

Nach der ersten Anmeldung mit `admin` und dem Startpasswort folgt ein Passwortwechsel. Danach unter „Benutzer & Rechte“ die benötigten Kassierer und Lageristen anlegen. Neue Benutzer erhalten ein Startpasswort und ändern es beim ersten Login. Ein Admin kann mehrere Rollen sowie zusätzliche oder verweigerte Einzelrechte vergeben. Eine Verweigerung hat Vorrang vor den Rollenrechten.

Nur im ausdrücklich aktivierten Entwicklungsmodus (`KASSENSYSTEM_AUTH_DEMO=true`) werden `admin`, `kassierer` und `lagerist` mit Passwort `1234` angelegt. Dieser Modus befüllt nur eine noch leere Benutzertabelle.

**Verkauf:** Fünf Schnellplätze zeigen angeheftete Favoriten und danach häufig verkaufte verfügbare Produkte. Der Produktname wählt das Produkt für eine eigene Menge aus; „+ 1“ legt direkt eine Einheit in den Warenkorb. Im eingeblendeten Bereich „Produktauswahl“ sind Bilder, Suche und Filter nach Kategorie, Einheit, Steuer, Preis und Verfügbarkeit verfügbar. Die Auswahl zeigt höchstens 18 Produkte pro Seite; „☆ Merken“ heftet bis zu fünf Favoriten lokal an. Im Warenkorb Mengen ändern und „Kauf abschließen“ wählen. Der Kaufabschluss erscheint als Einblendung in der Kasse; der Bon steht rechts und bleibt in der Historie erhalten.

**Bon speichern:** Den aktuellen Bon oder einen Eintrag der Bon-Historie auswählen. „Speichern“ öffnet die Dateiauswahl mit PDF und TXT. Der gewählte Dateityp bestimmt das Ausgabeformat. Ein abgebrochener Dialog verändert den Bon nicht.

**Verwaltung:** Der Verwaltungstab lädt sich beim Öffnen automatisch und aktualisiert die Produkte beim Wechsel zum Tab sowie regelmäßig im Hintergrund. Die Startseitenaktionen springen zum Produktformular, Warenzugang oder Bestand. Der Produktbereich und der Warenzugang sind aufklappbar. Beim Warenzugang schließt sich das Produktformular; nach Auswahl eines Produkts zeigt die rechte Seite nur dessen Namen, aktuellen Bestand und das Mengenfeld. Bei der Produktbearbeitung wechselt der Titel von „Neues Produkt“ zu „Produkt bearbeiten“; „Neues Produkt“ leert das Formular. Admins können Produkte anlegen, auswählen, bearbeiten und löschen. Die Kategorie wird aus einer festen Liste ausgewählt und lässt sich in Verwaltung und Kasse filtern; ältere frei eingegebene Werte erscheinen als „Sonstiges“. Für ein Produktbild eine PNG-, JPEG- oder GIF-Datei bis 5 MB auswählen; nach dem Upload das Produkt speichern. Die Bilder liegen zusammen mit der Datenbank im Docker-Volume beziehungsweise im lokalen Datenverzeichnis. Admins und Lageristen wählen ein Produkt und buchen im Bereich „Warenzugang“ eine positive Menge. Die Produktliste kann auch mit Tab und Enter bedient werden. Der Dunkelmodus richtet sich beim ersten Start unter Windows nach der Systemeinstellung; eine spätere Auswahl bleibt gespeichert. Das Zahnrad öffnet Einstellungen und Abmelden. Das Globussymbol daneben öffnet die Verwaltung im Browser. Beide Symbole erklären sich per Tooltip; das Fenster wird über das Fenstersymbol geschlossen.

**Fenstersteuerung:** Unter Windows mit Unterstützung für JavaFX 26 `StageStyle.EXTENDED` sitzen Minimieren, Maximieren und Schließen in der farblich passenden Kopfzeile neben den Einstellungen. Freie Bereiche der Kopfzeile lassen sich zum Verschieben verwenden; die Fensterrahmen bleiben für Größenänderungen und Windows-Snap erhalten. Auf Plattformen ohne diese Unterstützung bleibt die normale System-Titelleiste sichtbar. Nach einem Release die ZIP in einen neuen Ordner entpacken, damit die aktualisierte JavaFX-Laufzeit mitgestartet wird.

## Daten und Tests

Bei lokalem Maven-Start liegt die Datenbank unter `data/kassensystem.db`. Ein anderer Backend-Pfad kann mit `-Dkassensystem.db.path=<pfad>` oder `KASSENSYSTEM_DB_PATH=<pfad>` gesetzt werden. Benutzer, Produkte und Bons liegen in dieser Datei; hochgeladene Produktbilder im benachbarten Ordner `images`. Die geprüfte Anleitung für Sicherung und Wiederherstellung steht in [docs/BACKUP.md](docs/BACKUP.md).

```powershell
mvn verify
```

Die Tests prüfen Kassenlogik, Transaktions-Rollback, historische Bons, PDF-Inhalt, API und Rollen. Für die manuelle Abnahme einen Testkauf durchführen, den Bestand prüfen, die Anwendung neu starten und denselben Bon erneut öffnen sowie als PDF und TXT speichern.

Der Quality Gate prüft alle Tests und verlangt mindestens 20 % Zeilenabdeckung pro Modul. GitHub Actions prüft außerdem das Browser-JavaScript und baut das Docker-Image. Die API-Endpunkte und Beispiele stehen in [docs/API.md](docs/API.md).

## Screenshot-Plätze für die Abgabe

| Ansicht | Hier ein Bild ergänzen |
|---|---|
| Kasse | Produktwahl, Warenkorb und Bon nach einem Testkauf |
| Verwaltung als Admin | Produktliste und Produktformular |
| Verwaltung als Lagerist | Produktliste und Warenzugang |
| PDF-Bon | Geöffneter exportierter Bon mit Steueraufschlüsselung |

Die Screenshots können unter `docs/screenshots/` abgelegt und hier verlinkt werden. Die manuelle Abnahme der bisherigen Demo-Funktionen wurde am 2. Oktober 2026 vom Projektverantwortlichen bestätigt.

## Projektteile

- `kassensystem-javafx`: Kasse, Bon-Historie, PDF- und TXT-Speicherung
- `kassensystem-backend`: REST-API und Browser-Verwaltung
- `PFLICHTENHEFT.md`: Anforderungen und Abnahme
- `TODO.md`: laufende Restarbeiten
- `projektplan_agent.md`: Architektur und aktueller Projektstand
