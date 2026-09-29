# Kassensystem

Grafisches Kassensystem für einen kleinen Laden. Die JavaFX-App nutzt die REST-API des Spring-Boot-Backends für Produkte, Verkäufe und Bons. Nur das Backend greift auf SQLite zu.

## Schnellstart mit Docker

Unter Windows Docker Desktop installieren: `winget install -e --id Docker.DockerDesktop` (in UniGetUI nach `Docker.DockerDesktop` suchen). Docker Desktop starten. JDK 24 und Maven 3.9+ werden für den lokalen Build benötigt:

```powershell
mvn verify
docker compose up --build -d
mvn -pl kassensystem-javafx javafx:run
```

Die Verwaltung ist unter `http://127.0.0.1:8080/kassensystem/admin/` erreichbar. Die Daten liegen im Docker-Volume `kassensystem-data`. `docker compose down` stoppt das Backend und erhält die Daten. Die Kasse erreicht das Backend standardmäßig über `127.0.0.1:8080`; eine Hotspot-Verbindung ändert diese lokale Adresse nicht. Die Server-Wurzel `/` zeigt keine Seite.

Für ein anderes Backend `KASSENSYSTEM_API_URL` oder `-Dkassensystem.api.url=<URL>` setzen. Die URL enthält den Kontextpfad, etwa `http://localhost:8080/kassensystem`. Bei einer Release-ZIP die App mit `Kassensystem.exe` starten; Docker/Backend muss zuvor laufen.

## Downloads auf GitHub

Nach einem Tag wie `v1.0.0` veröffentlicht der Release-Workflow die ZIP `Kassensystem-Windows.zip` unter GitHub Releases und das Backend-Image als `ghcr.io/v3rfr3ss3n/kassensystemkomplett/backend:<tag>`. Ein Tag mit `-rc.` wird als Vorabversion markiert. Die ZIP enthält eine Java-Laufzeit. Vor dem ersten Release ist der Download als Artefakt eines manuell gestarteten Release-Workflows möglich. Container und Windows-Paket werden erst nach einem erfolgreichen Workflow-Lauf bereitgestellt.

## Voraussetzungen und Start

- JDK **24**
- Apache Maven **3.9 oder neuer**
- Windows für das bereitgestellte Startskript; unter anderen Systemen die Maven-Befehle verwenden.

Unter Windows `start-kassensystem.cmd` im Projektordner ausführen. Das Skript prüft JDK und Maven, startet das Backend im Hintergrund und danach die JavaFX-Kasse. Die Startmeldungen des Backends stehen in `data/backend-start.log`. Dies ist die Entwicklung ohne Docker.

Alternativ in zwei Terminals aus dem Projektordner starten:

```powershell
mvn -pl kassensystem-backend spring-boot:run
mvn -pl kassensystem-javafx javafx:run
```

In einer IDE das Maven-Projekt am Wurzelordner öffnen, JDK 24 konfigurieren und die Klassen `KassensystemBackendApplication` und `Main` starten. Der Browser-Adminbereich liegt unter `http://localhost:8080/kassensystem/admin/`.

## Anmelden und bedienen

| Nutzer | Passwort | Zugriff |
|---|---|---|
| `admin` | `1234` | Kasse und Verwaltung |
| `kassierer` | `1234` | Nur Kasse |
| `lagerist` | `1234` | Produktliste und Warenzugang |

**Verkauf:** In der Kasse ein Produkt suchen oder filtern, per `+` direkt hinzufügen oder auswählen und eine Menge eingeben. Im Warenkorb Mengen ändern und „Kauf abschließen“ wählen. Der Bon erscheint rechts und bleibt in der Historie erhalten.

**Bon ausgeben:** Den aktuellen Bon oder einen Eintrag der Bon-Historie auswählen. „Als PDF speichern“ öffnet die Dateiauswahl, „Drucken“ den Systemdruckdialog. Ein abgebrochener Dialog verändert den Bon nicht.

**Verwaltung:** Admins können Produkte anlegen, auswählen, bearbeiten und löschen. Admins und Lageristen wählen ein Produkt und buchen im Bereich „Warenzugang“ eine positive Menge. Die Produktliste kann auch mit Tab und Enter bedient werden. Nach Änderungen in der Webverwaltung die Produktliste der Kasse über „Aktualisieren“ neu laden.

## Daten und Tests

Bei lokalem Maven-Start liegt die Datenbank unter `data/kassensystem.db`. Ein anderer Backend-Pfad kann mit `-Dkassensystem.db.path=<pfad>` oder `KASSENSYSTEM_DB_PATH=<pfad>` gesetzt werden. Vor dem Austausch oder Zurücksetzen der Datenbank das Backend schließen und eine Kopie der Datei aufbewahren.

```powershell
mvn verify
```

Die Tests prüfen Kassenlogik, Transaktions-Rollback, historische Bons, PDF-Inhalt, API und Rollen. Für die manuelle Abnahme einen Testkauf durchführen, den Bestand prüfen, die Anwendung neu starten und denselben Bon erneut öffnen, exportieren und drucken.

Der Quality Gate prüft alle Tests und verlangt mindestens 20 % Zeilenabdeckung pro Modul. GitHub Actions prüft außerdem das Browser-JavaScript und baut das Docker-Image. Die API-Endpunkte und Beispiele stehen in [docs/API.md](docs/API.md).

## Screenshot-Plätze für die Abgabe

| Ansicht | Hier ein Bild ergänzen |
|---|---|
| Kasse | Produktwahl, Warenkorb und Bon nach einem Testkauf |
| Verwaltung als Admin | Produktliste und Produktformular |
| Verwaltung als Lagerist | Produktliste und Warenzugang |
| PDF-Bon | Geöffneter exportierter Bon mit Steueraufschlüsselung |

Die Screenshots können nach der manuellen Abnahme unter `docs/screenshots/` abgelegt und hier verlinkt werden.

## Projektteile

- `kassensystem-javafx`: Kasse, Bon-Historie, PDF und Druck
- `kassensystem-backend`: REST-API und Browser-Verwaltung
- `PFLICHTENHEFT.md`: Anforderungen und Abnahme
- `TODO.md`: laufende Restarbeiten
- `projektplan_agent.md`: Architektur und aktueller Projektstand
