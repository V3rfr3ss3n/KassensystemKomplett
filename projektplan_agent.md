# Projektplan Agent – Kassensystem mit Java 21, Maven und JavaFX

## 0. Projektziel

Dieses Projekt setzt das Lastenheft **„Entwicklung eines Kassenprogramms“** als grafisches Kassensystem um.

Ziel ist ein kleines, übersichtliches Kassensystem für einen Laden. Das Programm soll Produkte verwalten, Kassenvorgänge durchführen, Warenzugänge erfassen, Lagerbestände anzeigen und nach einem Verkauf einen Bon auf dem Bildschirm ausgeben.

Die Lehrkraft hat bestätigt, dass die verwendete Technologie frei gewählt werden darf und nicht bewertet wird. Deshalb wird nicht streng prozedural gearbeitet, sondern mit einer sauberen objektorientierten Struktur.

Da nur ca. **6 Schulstunden** zur Verfügung stehen und zwei Personen am Projekt arbeiten, gilt folgende Reihenfolge:

1. **MVP fertigstellen:** JavaFX-Desktop-App mit allen Pflichtfunktionen, Daten nur zur Laufzeit.
2. **Qualität verbessern:** bessere Fehlerbehandlung, saubere Oberfläche, Kommentare, README.
3. **Optionale Erweiterungen:** JSON-Speicherung, SQLite, Produktbearbeitung.
4. **Spätere große Erweiterung:** Spring-Boot-REST-API mit Browser-GUI.

Wichtig: **Spring Boot und Browser-GUI werden erst begonnen, wenn der lokale JavaFX-MVP vollständig funktioniert.**

---

## 1. Technologieentscheidung

### Gewählte Haupttechnologie für den MVP

| Bereich | Entscheidung |
|---|---|
| Sprache | Java |
| Version | Java 21 |
| Build-Tool | Maven |
| Oberfläche | JavaFX |
| IDE | VS Code |
| Architektur | Objektorientiert, MVC-ähnlich |
| Datenhaltung am Anfang | Nur im Arbeitsspeicher |
| Startdaten | Dummy-Produkte im Code |
| Produktnummern | intern automatisch generiert |
| Bon | zuerst nur auf dem Bildschirm |
| Server | nicht im MVP |
| Speicherung später | JSON, danach optional SQLite |
| Web/API später | Spring Boot + Browser-GUI |

### Warum JavaFX + Maven?

JavaFX ist für dieses Projekt sehr passend, weil damit direkt eine grafische Desktop-Anwendung ohne Browser gebaut werden kann. Maven sorgt dafür, dass Abhängigkeiten wie JavaFX sauber verwaltet werden und das Projekt in VS Code einfacher nachvollziehbar bleibt.

Vorteile:

- passt gut zu Java 21
- grafische Oberfläche ohne Browser
- objektorientierte Struktur möglich
- gute Trennung zwischen UI, Logik und Datenhaltung
- später erweiterbar durch JSON, Datenbank oder Server
- Maven erleichtert Start und Abgabe

Nachteile:

- JavaFX-Setup in VS Code kann etwas Zeit kosten
- Design ist nicht ganz so schnell wie bei HTML/CSS
- für Serverbetrieb später nicht ideal, deshalb später Spring Boot + Web-GUI

---

## 2. Framework- und Sprachvergleich

Dieser Abschnitt dient als Begründung, warum JavaFX gewählt wurde und welche Alternativen möglich wären.

| Technologie | Vorteile | Nachteile | Bewertung für dieses Projekt |
|---|---|---|---|
| Java 21 + JavaFX + Maven | bekannt, passend zur Berufsschule, Desktop-GUI, gute OOP-Struktur | Setup etwas aufwendiger | **Beste Wahl für MVP** |
| Java Swing | einfach, direkt in Java verfügbar | altmodische Oberfläche, unübersichtlicher GUI-Code | möglich, aber schlechter als JavaFX |
| Python + Tkinter | sehr schnell, einfach | weniger passend zu Java-Unterricht, einfache Optik | schnell, aber nicht ideal |
| Python + PySide6/PyQt | moderne Desktop-GUI | neue Technik, Setup-Zeit | technisch gut, aber riskant bei 6 Schulstunden |
| HTML/CSS/JavaScript | schnelle schöne GUI, gut für Web | anderer Projekttyp, mehrere Technologien | gut für spätere Browser-GUI |
| C# + WPF/Avalonia | professionelle Desktop-GUI | neue Sprache, neues Setup | stark, aber zu viel Umstieg |
| Java + Spring Boot + Browser-GUI | sehr gut für Server/API, moderne Web-App | mehr Aufwand, Frontend nötig | **später sehr gut, nicht für MVP** |

### Endentscheidung

Für die Abgabe wird zuerst gebaut:

> **Java 21 + Maven + JavaFX als lokale Desktop-App**

Wenn danach noch Zeit bleibt oder das Projekt später erweitert wird:

> **Spring Boot REST API + Browser-GUI mit HTML/CSS/JavaScript**

---

## 3. Projektpriorität

### Muss fertig werden

Diese Punkte erfüllen das Lastenheft und haben höchste Priorität:

- Maven-Projekt startet in VS Code
- JavaFX-Hauptfenster öffnet sich
- Produktliste anzeigen
- Kassenvorgang starten
- Produkt auswählen
- Menge eingeben
- Lagerbestand prüfen
- Produkt zum Warenkorb hinzufügen
- Gesamtpreis berechnen
- Kauf abschließen
- Bon anzeigen
- Lagerbestand nach Verkauf aktualisieren
- Neues Produkt hinzufügen
- Warenzugang erfassen
- Lagerbestand anzeigen
- Fehlerhafte Eingaben abfangen

### Sollte fertig werden

Diese Punkte verbessern Qualität und Präsentation:

- saubere Ordnerstruktur
- gute Klassennamen
- kurze Methoden
- wichtige Kommentare
- Bon mit Datum, Uhrzeit und Bonnummer
- übersichtliches Layout
- CSS-Datei für JavaFX
- README mit Startanleitung

### Optional, nur wenn Zeit übrig ist

Diese Punkte sind Erweiterungen:

- Produkt bearbeiten
- Produkt löschen
- JSON-Speicherung
- Bon-Historie
- SQLite-Datenbank
- Spring-Boot-API
- Browser-GUI
- mehrere Clients

---

## 4. Empfohlene Projektstruktur für Maven

```text
kassensystem/
├── README.md
├── projektplan_agent.md
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── de/
        │       └── mmbbs/
        │           └── kassensystem/
        │               ├── Main.java
        │               ├── app/
        │               │   └── KassenApp.java
        │               ├── model/
        │               │   ├── Produkt.java
        │               │   ├── Bon.java
        │               │   └── BonPosition.java
        │               ├── repository/
        │               │   ├── ProduktRepository.java
        │               │   └── InMemoryProduktRepository.java
        │               ├── service/
        │               │   ├── ProduktService.java
        │               │   ├── KassenService.java
        │               │   └── BonService.java
        │               ├── ui/
        │               │   ├── MainView.java
        │               │   ├── KassenView.java
        │               │   ├── ProduktFormView.java
        │               │   └── LagerView.java
        │               └── util/
        │                   ├── GeldFormatter.java
        │                   └── DialogUtil.java
        └── resources/
            └── styles.css
```

Für den Anfang darf die Struktur kleiner sein. Wichtig ist die klare Trennung:

- `model` enthält Datenklassen.
- `repository` speichert und lädt Produkte.
- `service` enthält Programmlogik.
- `ui` enthält JavaFX-Oberflächen.
- `util` enthält Hilfsklassen.

---

## 5. Architekturidee

Das Programm wird in vier Hauptbereiche aufgeteilt.

### 5.1 Model

Die Model-Klassen enthalten nur Daten und einfache Regeln. Sie kennen keine JavaFX-Elemente.

#### Produkt

Aufgabe: Speichert alle Informationen zu einem Produkt.

Attribute:

- `int id`
- `String name`
- `double preis`
- `int lagerbestand`

Wichtige Methoden:

- Konstruktor
- Getter
- Setter für Name, Preis und Lagerbestand
- `bestandErhoehen(int menge)`
- `bestandVerringern(int menge)`

Regeln:

- Name darf nicht leer sein.
- Preis muss größer als 0 sein.
- Lagerbestand darf nicht negativ sein.
- Produktnummer wird automatisch vergeben.

#### BonPosition

Aufgabe: Speichert eine Zeile auf dem Bon.

Attribute:

- `Produkt produkt`
- `int menge`
- `double einzelpreis`
- `double gesamtpreis`

Regeln:

- Menge muss größer als 0 sein.
- Gesamtpreis = Menge × Einzelpreis.
- Einzelpreis wird beim Kauf gespeichert, damit spätere Preisänderungen alte Bons nicht verändern.

#### Bon

Aufgabe: Speichert einen abgeschlossenen Kauf.

Attribute:

- `int bonnummer`
- `LocalDateTime datumUhrzeit`
- `List<BonPosition> positionen`
- `double gesamtpreis`

Regeln:

- Bon wird erst beim Abschluss erzeugt.
- Bon enthält alle gekauften Produkte.
- Bon reduziert nicht selbst den Lagerbestand. Das macht der `KassenService`.

---

### 5.2 Repository

Das Repository verwaltet die Produktdaten.

Am Anfang wird alles nur im Arbeitsspeicher gespeichert.

#### ProduktRepository

Schnittstelle für spätere Erweiterungen.

Methoden:

- `List<Produkt> findeAlle()`
- `Optional<Produkt> findeNachId(int id)`
- `Produkt speichern(Produkt produkt)`
- `void loeschen(int id)` optional

#### InMemoryProduktRepository

Speichert Produkte in einer `ArrayList` oder `HashMap`.

Am Anfang werden Dummy-Produkte erzeugt:

- Cola
- Wasser
- Brötchen
- Kaffee
- Schokoriegel

Später kann dieses Repository durch JSON oder Datenbank ersetzt werden.

---

### 5.3 Service

Services enthalten die eigentliche Programmlogik. Sie kennen keine JavaFX-Controls.

#### ProduktService

Aufgaben:

- neues Produkt anlegen
- Produkte anzeigen
- Warenzugang buchen
- Lagerbestand prüfen

Methoden:

- `produktHinzufuegen(String name, double preis, int anfangsbestand)`
- `warenzugangErfassen(int produktId, int menge)`
- `List<Produkt> alleProdukte()`
- `boolean istMengeVerfuegbar(int produktId, int menge)`

#### KassenService

Aufgaben:

- Kassenvorgang verwalten
- Produkte zum Warenkorb hinzufügen
- Lagerbestand prüfen
- Lagerbestand reduzieren
- Bon erzeugen

Methoden:

- `void positionHinzufuegen(int produktId, int menge)`
- `List<BonPosition> getWarenkorb()`
- `double berechneGesamtpreis()`
- `Bon kassenvorgangAbschliessen()`
- `void warenkorbLeeren()`

#### BonService

Aufgaben:

- Bonnummer vergeben
- Bontext erzeugen
- später Bon speichern

Methoden:

- `Bon erstelleBon(List<BonPosition> positionen)`
- `String formatiereBon(Bon bon)`

---

### 5.4 UI / JavaFX

Die UI zeigt Daten an und ruft Services auf.

#### MainView

Hauptmenü mit Buttons:

- Kasse
- Produkt hinzufügen
- Warenzugang
- Lagerbestand
- Beenden

#### KassenView

Funktionen:

- Produktliste anzeigen
- Produkt auswählen
- Menge eingeben
- zum Warenkorb hinzufügen
- Warenkorb anzeigen
- Gesamtpreis anzeigen
- Kauf abschließen
- Bon anzeigen

#### ProduktFormView

Funktionen:

- Name eingeben
- Preis eingeben
- Anfangsbestand eingeben
- Produkt speichern

#### LagerView

Funktionen:

- Tabelle mit Produkten
- Produktnummer
- Name
- Preis
- Lagerbestand
- Warenzugang buchen

---

## 6. Geplante Benutzeroberfläche

### Hauptfenster

```text
+--------------------------------------------------+
| Kassensystem                                     |
+----------------------+---------------------------+
| [Kasse]              | Inhalt                    |
| [Produkt hinzufügen] |                           |
| [Warenzugang]        |                           |
| [Lagerbestand]       |                           |
| [Beenden]            |                           |
+----------------------+---------------------------+
```

### Kassenansicht

```text
+--------------------------------------------------+
| Kassenvorgang                                    |
+--------------------------------------------------+
| Produkt auswählen: [Dropdown / Tabelle]          |
| Menge:            [Textfeld]                     |
| [Zum Warenkorb hinzufügen]                       |
+--------------------------------------------------+
| Warenkorb                                        |
| Produkt       Menge      Einzelpreis     Gesamt  |
| Cola          2          1,50 €          3,00 €  |
+--------------------------------------------------+
| Gesamtpreis: 3,00 €                              |
| [Kauf abschließen] [Abbrechen]                   |
+--------------------------------------------------+
```

### Bon-Anzeige

```text
Bon Nr. 1
Datum: 28.05.2026
Uhrzeit: 12:30

Produkt        Menge    Einzelpreis    Gesamt
Cola           2        1,50 €         3,00 €
Brötchen       3        0,60 €         1,80 €

Gesamtpreis: 4,80 €

Vielen Dank für Ihren Einkauf!
```

---

## 7. Maven-Setup

Die genaue `pom.xml` kann später vom Coding-Agenten erzeugt werden. Wichtig ist:

- Java-Version: 21
- JavaFX-Abhängigkeiten: `javafx-controls`, optional `javafx-fxml`
- Plugin zum Starten: `javafx-maven-plugin`

Empfohlener Startbefehl:

```bash
mvn clean javafx:run
```

Wenn FXML nicht genutzt wird, kann die UI komplett in Java-Code gebaut werden. Für 6 Schulstunden ist eine UI in Java-Code meistens schneller, weil keine zusätzlichen FXML-Dateien gepflegt werden müssen.

Empfehlung:

> **Keine FXML-Dateien im MVP. JavaFX-Oberfläche direkt in Java-Klassen bauen.**

Grund:

- weniger Dateien
- schnelleres Debugging
- einfachere Zusammenarbeit
- weniger Setup-Probleme

---

## 8. Ablauf Kassenvorgang

1. Benutzer öffnet Kassenansicht.
2. Produktliste wird angezeigt.
3. Benutzer wählt Produkt aus.
4. Benutzer gibt Menge ein.
5. Programm prüft Eingabe.
6. Programm prüft Lagerbestand.
7. Wenn genug Bestand vorhanden ist, wird Produkt zum Warenkorb hinzugefügt.
8. Benutzer kann weitere Produkte hinzufügen.
9. Benutzer klickt auf „Kauf abschließen“.
10. Programm reduziert Lagerbestand.
11. Programm erstellt Bon.
12. Bon wird auf dem Bildschirm angezeigt.
13. Warenkorb wird geleert.
14. Produkt- und Lageranzeigen werden aktualisiert.

---

## 9. Fehlerbehandlung

Diese Fehler müssen abgefangen werden:

| Situation | Reaktion |
|---|---|
| Name leer | Meldung: „Bitte Produktnamen eingeben.“ |
| Preis leer | Meldung: „Bitte Preis eingeben.“ |
| Preis ungültig | Meldung: „Preis muss eine Zahl größer als 0 sein.“ |
| Bestand leer | Meldung: „Bitte Bestand eingeben.“ |
| Bestand ungültig | Meldung: „Bestand muss eine ganze Zahl ab 0 sein.“ |
| Menge leer | Meldung: „Bitte Menge eingeben.“ |
| Menge ungültig | Meldung: „Menge muss eine ganze Zahl größer als 0 sein.“ |
| Produkt nicht ausgewählt | Meldung: „Bitte Produkt auswählen.“ |
| Nicht genug Bestand | Meldung: „Nicht genügend Produkte auf Lager.“ |
| Warenkorb leer | Meldung: „Der Warenkorb ist leer.“ |

---

## 10. Arbeitsteilung für 2 Personen

### Person A – UI / JavaFX

Aufgaben:

- Maven/JavaFX-Projekt lauffähig machen
- Hauptfenster erstellen
- Navigation bauen
- Kassenansicht bauen
- Produktformular bauen
- Lageransicht bauen
- Dialoge und Fehlermeldungen anzeigen
- CSS nur einbauen, wenn Zeit übrig ist

### Person B – Logik / Datenmodell

Aufgaben:

- Model-Klassen erstellen
- Repository erstellen
- Dummy-Produkte anlegen
- ProduktService bauen
- KassenService bauen
- BonService bauen
- Validierung unterstützen
- Geldformatierung und Bontext bauen

### Gemeinsame Aufgaben

- Services mit UI verbinden
- Testfälle durchführen
- Fehler beheben
- Kommentare ergänzen
- README schreiben
- Abgabeordner prüfen

---

## 11. Zeitplan für 6 Schulstunden

### Stunde 1 – Maven-Projekt und Grundstruktur

Ziele:

- Maven-Projekt in VS Code erstellen
- Java 21 prüfen
- JavaFX lauffähig machen
- `pom.xml` einrichten
- Ordnerstruktur anlegen
- Hauptfenster anzeigen

Ergebnis:

- Programm startet mit `mvn clean javafx:run`.
- JavaFX-Fenster öffnet sich.

---

### Stunde 2 – Datenmodell und Produktlogik

Ziele:

- `Produkt`, `BonPosition`, `Bon` erstellen
- `ProduktRepository` erstellen
- `InMemoryProduktRepository` mit Dummy-Produkten erstellen
- `ProduktService` fertigstellen
- Lagerbestand anzeigen

Ergebnis:

- Dummy-Produkte existieren im Speicher.
- Produkte können in der UI angezeigt werden.

---

### Stunde 3 – Produkte und Warenzugang

Ziele:

- Neues Produkt hinzufügen
- Warenzugang erfassen
- Eingaben prüfen
- Tabellen aktualisieren

Ergebnis:

- Neue Produkte können hinzugefügt werden.
- Lagerbestand kann erhöht werden.

---

### Stunde 4 – Kassenvorgang

Ziele:

- KassenView erstellen
- Produkt auswählen
- Menge eingeben
- zum Warenkorb hinzufügen
- Bestand prüfen
- Gesamtpreis anzeigen

Ergebnis:

- Produkte können in einen Warenkorb gelegt werden.
- Ungültige Mengen und zu kleiner Bestand werden abgefangen.

---

### Stunde 5 – Kaufabschluss und Bon

Ziele:

- Kauf abschließen
- Lagerbestand reduzieren
- Bon erzeugen
- Bon anzeigen
- Warenkorb leeren

Ergebnis:

- Ein vollständiger Kassenvorgang funktioniert.
- Bon wird auf dem Bildschirm angezeigt.

---

### Stunde 6 – Stabilisieren und Abgabe

Ziele:

- alle Testfälle durchgehen
- Bugs beheben
- Fehlermeldungen verbessern
- README schreiben
- wichtige Code-Kommentare ergänzen
- Projektordner kontrollieren

Ergebnis:

- Projekt ist abgabefertig.

---

## 12. MVP-Testfälle

Diese Testfälle müssen funktionieren, bevor Erweiterungen begonnen werden.

### Testfall 1: Programmstart

1. Programm starten.
2. Hauptfenster öffnet sich.
3. Keine Fehlermeldung erscheint.

Erwartung:

- Hauptmenü ist sichtbar.

### Testfall 2: Lagerbestand anzeigen

1. Lagerbestand öffnen.
2. Dummy-Produkte prüfen.

Erwartung:

- Produkte mit Produktnummer, Name, Preis und Lagerbestand werden angezeigt.

### Testfall 3: Neues Produkt hinzufügen

1. Produkt hinzufügen öffnen.
2. Name, Preis und Anfangsbestand eingeben.
3. Speichern klicken.
4. Lagerbestand öffnen.

Erwartung:

- Neues Produkt erscheint in der Liste.

### Testfall 4: Warenzugang erfassen

1. Produkt auswählen.
2. Menge eingeben.
3. Warenzugang buchen.
4. Lagerbestand prüfen.

Erwartung:

- Bestand wurde korrekt erhöht.

### Testfall 5: Verkauf durchführen

1. Kasse öffnen.
2. Produkt auswählen.
3. Menge eingeben.
4. Zum Warenkorb hinzufügen.
5. Kauf abschließen.

Erwartung:

- Bon wird angezeigt.
- Gesamtpreis ist korrekt.
- Lagerbestand wurde reduziert.

### Testfall 6: Zu hohe Verkaufsmenge

1. Produkt auswählen.
2. Menge eingeben, die größer als der Lagerbestand ist.
3. Hinzufügen klicken.

Erwartung:

- Fehlermeldung erscheint.
- Produkt wird nicht verkauft.
- Lagerbestand bleibt gleich.

### Testfall 7: Ungültige Eingabe

1. Bei Menge Text eingeben, zum Beispiel „abc“.
2. Hinzufügen klicken.

Erwartung:

- Fehlermeldung erscheint.
- Programm stürzt nicht ab.

---

## 13. Spätere Erweiterung: JSON-Speicherung

Wenn der MVP fertig ist, kann die Datenhaltung erweitert werden.

Neue Klasse:

```text
JsonProduktRepository
```

Aufgaben:

- Produkte aus `produkte.json` laden
- Produkte nach Änderungen speichern
- beim Programmstart vorhandene Datei lesen
- wenn Datei fehlt, Dummy-Produkte erzeugen

Mögliche JSON-Struktur:

```json
[
  {
    "id": 1,
    "name": "Cola",
    "preis": 1.5,
    "lagerbestand": 20
  },
  {
    "id": 2,
    "name": "Wasser",
    "preis": 1.0,
    "lagerbestand": 30
  }
]
```

Empfohlene Bibliothek:

- Gson für schnelle Schulprojekt-Umsetzung
- Jackson, falls später Spring Boot genutzt wird

Empfehlung:

> Für den MVP kein JSON. Danach zuerst Gson oder direkt Jackson, wenn Spring Boot geplant ist.

---

## 14. Spätere Erweiterung: SQLite-Datenbank

Wenn JSON funktioniert oder ersetzt werden soll, kann eine kleine Datenbank verwendet werden.

Empfehlung:

- SQLite

Tabellen:

### produkte

| Spalte | Typ |
|---|---|
| id | INTEGER PRIMARY KEY |
| name | TEXT |
| preis | REAL |
| lagerbestand | INTEGER |

### bons

| Spalte | Typ |
|---|---|
| id | INTEGER PRIMARY KEY |
| datum_uhrzeit | TEXT |
| gesamtpreis | REAL |

### bon_positionen

| Spalte | Typ |
|---|---|
| id | INTEGER PRIMARY KEY |
| bon_id | INTEGER |
| produkt_id | INTEGER |
| produkt_name | TEXT |
| menge | INTEGER |
| einzelpreis | REAL |
| gesamtpreis | REAL |

---

## 15. Spätere Erweiterung: Spring Boot + Browser-GUI

Wenn der lokale JavaFX-MVP fertig ist, kann eine zweite Version als Web-/Serverprojekt entstehen.

### Neue Zielarchitektur

```text
Browser-GUI  --->  Spring Boot REST API  --->  Service-Schicht  --->  Datenbank/Repository
HTML/CSS/JS       Java 21 + Maven             Java-Logik           JSON/SQLite/H2
```

### Wichtig

In dieser Erweiterung wird JavaFX nicht mehr als Client verwendet. Stattdessen gibt es eine Browser-GUI.

Grund:

- Spring Boot passt natürlich zu Browser und REST-API.
- Eine Web-GUI ist leichter mit API-Endpunkten zu verbinden.
- Mehrere Clients können einfach über den Browser zugreifen.
- Die Kassenlogik kann teilweise aus dem JavaFX-MVP übernommen werden.

### Mögliche Web-Technik

Für eine einfache Browser-GUI:

- HTML
- CSS
- JavaScript
- `fetch()` für API-Aufrufe

Optional später:

- Thymeleaf, wenn alles serverseitig gerendert werden soll
- React/Vue, wenn eine moderne Single-Page-App gewünscht ist

Für das Schulprojekt ist am einfachsten:

> **Spring Boot REST API + einfache HTML/CSS/JavaScript-GUI**

### API-Endpunkte

```text
GET    /api/produkte
POST   /api/produkte
POST   /api/produkte/{id}/warenzugang
GET    /api/lagerbestand
POST   /api/kasse/abschluss
GET    /api/bons
```

### Ablauf mit Web/API

1. Browser-GUI startet.
2. JavaScript lädt Produkte über `GET /api/produkte`.
3. Benutzer verkauft Produkte in der Browser-GUI.
4. Browser sendet Verkauf an `POST /api/kasse/abschluss`.
5. Spring Boot prüft Bestand.
6. Spring Boot reduziert Bestand.
7. Spring Boot erzeugt Bon.
8. Browser zeigt Bon an.

### Wiederverwendbare Teile aus MVP

Diese Klassen können später übernommen oder angepasst werden:

- `Produkt`
- `Bon`
- `BonPosition`
- `ProduktService`
- `KassenService`
- `BonService`
- Repository-Idee

Diese Teile werden ersetzt:

- JavaFX-Views
- JavaFX-Dialoge
- lokale UI-Navigation

---

## 16. Coding-Regeln für den Agenten

Der Agent soll diese Regeln beachten:

1. Erst MVP umsetzen, keine Erweiterungen vorziehen.
2. Java 21 verwenden.
3. Maven verwenden.
4. JavaFX für den lokalen MVP verwenden.
5. Keine Spring-Boot-Struktur im MVP einbauen.
6. Keine Browser-GUI im MVP einbauen.
7. Saubere Klassenstruktur einhalten.
8. UI und Logik trennen.
9. Services dürfen keine JavaFX-Elemente kennen.
10. Model-Klassen sollen keine UI-Logik enthalten.
11. Repository austauschbar halten.
12. Fehlerbehandlung direkt einbauen.
13. Methoden kurz und verständlich halten.
14. Namen auf Deutsch oder Englisch konsistent verwenden.
15. Geldbeträge mit zwei Nachkommastellen anzeigen.
16. Code so kommentieren, dass Mitschüler und Lehrkraft ihn verstehen.
17. Nach jedem erledigten Block den Abschnitt „Erledigt“ aktualisieren.

---

## 17. Namenskonvention

Empfehlung: Deutsch für fachliche Begriffe, weil das Lastenheft deutsch ist.

Beispiele:

- `Produkt`
- `Bon`
- `BonPosition`
- `ProduktService`
- `KassenService`
- `ProduktRepository`
- `InMemoryProduktRepository`
- `KassenView`
- `LagerView`

Methoden:

- `produktHinzufuegen`
- `warenzugangErfassen`
- `alleProdukte`
- `kassenvorgangAbschliessen`
- `berechneGesamtpreis`

Hinweis: Umlaute in Methodennamen vermeiden. Also besser `hinzufuegen` statt `hinzufügen`.

---

## 18. Offene Risiken

### Risiko 1: JavaFX in VS Code braucht Setup-Zeit

Problem:

- JavaFX läuft nicht automatisch in jedem VS-Code-Projekt.

Lösung:

- Maven verwenden.
- Direkt am Anfang `mvn clean javafx:run` testen.
- Wenn JavaFX nicht startet, zuerst nur minimales Fenster lauffähig bekommen.

### Risiko 2: Zu viele Erweiterungen

Problem:

- JSON, DB, Spring Boot und Browser-GUI kosten viel Zeit.

Lösung:

- Erst MVP fertig machen.
- Erweiterungen nur nach erfolgreichen Testfällen 1 bis 7 beginnen.

### Risiko 3: Zu viel UI-Design

Problem:

- Design kann Zeit fressen, ohne Pflichtfunktionen zu verbessern.

Lösung:

- Erst funktionale Oberfläche bauen.
- CSS und schönes Layout erst nach vollständiger Funktion.

---

## 19. Sofort umzusetzende Reihenfolge

Diese Reihenfolge ist verbindlich, damit keine Zeit verloren geht.

1. Maven-Projekt starten.
2. JavaFX-Startfenster lauffähig machen.
3. `Produkt` erstellen.
4. `BonPosition` erstellen.
5. `Bon` erstellen.
6. `ProduktRepository` erstellen.
7. `InMemoryProduktRepository` mit Dummy-Produkten erstellen.
8. `ProduktService` erstellen.
9. `KassenService` erstellen.
10. `BonService` erstellen.
11. Hauptfenster mit Navigation erstellen.
12. Lageransicht erstellen.
13. Produkt-hinzufügen-Ansicht erstellen.
14. Warenzugang-Funktion erstellen.
15. Kassenansicht erstellen.
16. Warenkorb einbauen.
17. Bonanzeige einbauen.
18. Fehlerbehandlung prüfen.
19. README und Kommentare ergänzen.
20. Erst danach optionale Erweiterungen starten.

---

# 20. Todo

## Muss-Aufgaben

- [ ] Maven-Projekt in VS Code mit Java 21 erstellen
- [ ] `pom.xml` mit JavaFX einrichten
- [ ] Start mit `mvn clean javafx:run` testen
- [ ] Projektstruktur anlegen
- [ ] `Main.java` erstellen
- [ ] `KassenApp.java` erstellen
- [ ] `Produkt` erstellen
- [ ] `BonPosition` erstellen
- [ ] `Bon` erstellen
- [ ] `ProduktRepository` erstellen
- [ ] `InMemoryProduktRepository` erstellen
- [ ] Dummy-Produkte im Repository anlegen
- [ ] `ProduktService` erstellen
- [ ] `KassenService` erstellen
- [ ] `BonService` erstellen
- [ ] Hauptmenü mit JavaFX bauen
- [ ] Lagerbestand anzeigen
- [ ] Neues Produkt hinzufügen
- [ ] Warenzugang erfassen
- [ ] Kassenvorgang starten
- [ ] Produkt auswählen
- [ ] Menge eingeben
- [ ] Lagerbestand prüfen
- [ ] Produkt zum Warenkorb hinzufügen
- [ ] Gesamtpreis anzeigen
- [ ] Kauf abschließen
- [ ] Lagerbestand reduzieren
- [ ] Bon mit Datum, Uhrzeit und Bonnummer anzeigen
- [ ] Warenkorb nach Kauf leeren
- [ ] Fehlerhafte Eingaben abfangen
- [ ] Testfälle 1 bis 7 durchführen
- [ ] Code kommentieren
- [ ] README schreiben
- [ ] Abgabeordner kontrollieren

## Sollte-Aufgaben

- [ ] Oberfläche optisch verbessern
- [ ] CSS-Datei einbauen
- [ ] Tabellen automatisch aktualisieren
- [ ] Einheitliche Dialoge verwenden
- [ ] Geldbeträge einheitlich formatieren
- [ ] Bon schöner formatieren
- [ ] Produktnummern sichtbar machen

## Optionale Aufgaben nach MVP

- [ ] Produkt bearbeiten
- [ ] Produkt löschen
- [ ] Produkte in JSON speichern
- [ ] Produkte aus JSON laden
- [ ] Bons speichern
- [ ] SQLite-Datenbank einbauen

## Optionale große Erweiterung

- [ ] Spring-Boot-Projekt planen
- [ ] REST-API-Endpunkte definieren
- [ ] Spring-Boot-Backend erstellen
- [ ] Browser-GUI mit HTML/CSS/JavaScript erstellen
- [ ] Produkte über API laden
- [ ] Kassenvorgang über API abschließen
- [ ] Datenbank an Spring Boot anbinden

---

# 21. Erledigt

Dieser Abschnitt wird vom Agenten oder Team nach jeder abgeschlossenen Aufgabe aktualisiert.

## Bereits erledigt

- [x] Anforderungen aus dem Lastenheft analysiert
- [x] Projektumfang festgelegt
- [x] Lehrkraft hat freie Technologiewahl bestätigt
- [x] Java 21 als Zielversion festgelegt
- [x] Maven als Build-Tool festgelegt
- [x] JavaFX als GUI-Technik für den MVP gewählt
- [x] Lokaler MVP ohne Server festgelegt
- [x] Spätere Erweiterungen mit JSON, Datenbank und API eingeplant
- [x] Spring Boot später nur mit Browser-GUI geplant
- [x] Projektplan erstellt und überarbeitet

## Entscheidungen

- GUI wird von Anfang an mit JavaFX umgesetzt.
- Build-Tool ist Maven.
- Server kommt erst nach dem lokalen MVP.
- Wenn Spring Boot genutzt wird, dann mit Browser-GUI statt JavaFX-Client.
- Daten werden am Anfang nur zur Laufzeit gespeichert.
- Dummy-Produkte werden im Code erzeugt.
- Produktnummern werden intern automatisch vergeben.
- Bon wird zuerst nur auf dem Bildschirm angezeigt.
- Bon enthält Bonnummer, Datum und Uhrzeit.
- Code soll objektorientiert, sauber und verständlich sein.
- FXML wird im MVP nicht empfohlen, damit die Umsetzung schneller bleibt.

## Notizen für spätere Bearbeitung

- Keine Zeit mit Spring Boot verlieren, solange die JavaFX-App nicht komplett funktioniert.
- Wenn JavaFX-Setup Probleme macht, nur ein minimales Fenster starten und danach sofort Logik bauen.
- JSON, DB und Server erst beginnen, wenn der Kassenvorgang vollständig funktioniert.
- Für Spring Boot später möglichst viel Logik aus den Services übernehmen.

---

# 22. Definition of Done

Das Projekt gilt als fertig, wenn folgende Punkte erfüllt sind:

- Programm startet ohne Fehler über Maven.
- Hauptmenü ist sichtbar.
- Produkte können angezeigt werden.
- Neue Produkte können hinzugefügt werden.
- Warenzugang kann erfasst werden.
- Kassenvorgang kann durchgeführt werden.
- Zu geringer Lagerbestand wird erkannt.
- Ungültige Eingaben werden abgefangen.
- Bon wird mit Positionen und Gesamtpreis angezeigt.
- Lagerbestand wird nach Verkauf aktualisiert.
- Code ist sinnvoll strukturiert.
- Wichtige Stellen sind kommentiert.
- README erklärt Start und Bedienung.
- Projektordner enthält alle notwendigen Dateien.

---

# 23. Kurzfassung für die Abgabe

Wir entwickeln ein grafisches Kassensystem mit **Java 21, Maven und JavaFX**. Das Programm verwaltet Produkte mit Name, Preis und Lagerbestand. Benutzer können Produkte hinzufügen, Warenzugänge buchen, den Lagerbestand anzeigen und Kassenvorgänge durchführen. Beim Verkauf prüft das Programm den Lagerbestand, berechnet den Gesamtpreis, reduziert den Bestand und zeigt einen Bon mit Datum, Uhrzeit, Bonnummer, Positionen und Gesamtpreis an. Die Daten werden zunächst nur während der Laufzeit gespeichert. Die Architektur ist objektorientiert aufgebaut und so vorbereitet, dass später JSON-Speicherung, SQLite oder eine Spring-Boot-REST-API mit Browser-GUI ergänzt werden können.
