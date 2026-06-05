# Pflichtenheft: Kassensystem

## 1. Ausgangslage

Grundlage ist das Lastenheft "Entwicklung eines Kassenprogramms". Das Programm soll die wichtigsten Arbeitsablaeufe einer kleinen Ladenkasse abbilden: Produkte verwalten, Kassenvorgaenge durchfuehren, Wareneingaenge buchen, Lagerbestaende anzeigen und Bons ausgeben.

Das bestehende Projekt setzt diese Anforderungen als JavaFX-Desktopanwendung um. Administrative Funktionen fuer Produkte und Lager koennen zusaetzlich ueber ein Spring-Boot-Backend mit Browseroberflaeche genutzt werden. JavaFX und Spring Boot verwenden dieselbe SQLite-Datenbank.

## 2. Zielbestimmung

### Muss-Kriterien

- Das Programm zeigt nach dem Start eine bedienbare Oberflaeche fuer die Kasse an.
- Produkte besitzen mindestens Name, Preis, Produktnummer und Lagerbestand.
- Produkte koennen verkauft werden.
- Beim Verkauf wird die gewaehlte Menge gegen den Lagerbestand geprueft.
- Der Gesamtpreis wird berechnet und angezeigt.
- Nach dem Kauf wird der Lagerbestand reduziert.
- Nach dem Kauf wird ein Bon mit Positionen, Mengen, Einzelpreisen und Gesamtpreis ausgegeben.
- Neue Produkte koennen angelegt werden.
- Warenzugaenge koennen den Lagerbestand erhoehen.
- Der aktuelle Lagerbestand kann angezeigt werden.
- Ungueltige Eingaben werden abgefangen und als Meldung angezeigt.
- Der Code ist in Methoden, Klassen und Schichten aufgeteilt und dokumentiert.

### Soll-Kriterien

- Bon-Historie bleibt nach einem Neustart erhalten.
- Produktbilder koennen hinzugefuegt und zugeschnitten werden.
- Produkte koennen gesucht und gefiltert werden.
- Produkte koennen bearbeitet und geloescht werden.
- Umsatzsteuer und Verkaufseinheiten werden unterstuetzt.
- Administrative Funktionen sind ueber eine Weboberflaeche erreichbar.

### Kann-Kriterien

- Spring Boot stellt eine REST-API fuer spaetere Web- oder Mehrplatznutzung bereit.
- Die JavaFX-Anwendung kann den Adminbereich per WebView oder Browser oeffnen.
- Die Datenbank kann ueber einen Systemparameter oder eine Umgebungsvariable umgestellt werden.

## 3. Produkteinsatz

Das Kassensystem ist fuer einen kleinen Laden gedacht. Eine Person an der Kasse kann Produkte auswaehlen, in den Warenkorb legen und den Kauf abschliessen. Eine verwaltende Person kann Produkte und Lagerbestaende pflegen.

## 4. Produktfunktionen

### 4.1 Hauptmenue und Berechtigungssystem

- Nach dem Start erscheint eine Anmeldung.
- Testnutzer:
  - `admin / 1234`
  - `kassierer / 1234`
- Kassierer werden direkt in die Kasse geleitet und sehen keine Verwaltung.
- Admins sehen ein Hauptmenue mit den Optionen:
  - Kassenvorgang starten
  - Neues Produkt hinzufuegen
  - Warenzugang erfassen
  - Lagerbestand anzeigen
  - Programm beenden
- Administrative Optionen fuehren in den geschuetzten Verwaltungsbereich.

### 4.2 Kassenfunktion

- Produktliste anzeigen
- Produkte per Klick in den Warenkorb legen
- Menge erfassen
- Warenkorbpositionen erhoehen, verringern oder entfernen
- Lagerbestand vor dem Verkauf pruefen
- Gesamtpreis berechnen
- Kauf abschliessen
- Bon anzeigen und in der Historie speichern

### 4.3 Produktverwaltung

- Produkt mit Name, Preis, Anfangsbestand, Einheit, Steuer und optionalem Bild anlegen
- Produkt bearbeiten
- Produkt loeschen
- Produkte suchen und filtern

### 4.4 Warenzugang

- Produkt auswaehlen
- Menge eingeben
- Lagerbestand erhoehen
- Aktualisierten Bestand anzeigen

### 4.5 Lagerbestand

- Alle Produkte mit aktuellem Bestand anzeigen
- Bestand nach Kauf und Warenzugang aktualisieren
- Einheit passend anzeigen, zum Beispiel Stueck, kg, l oder Packung

### 4.6 Bon

- Bonnummer anzeigen
- Datum und Uhrzeit anzeigen
- Produktname, Menge, Steuer, Bruttobetrag und Gesamtbetrag anzeigen
- Netto- und Steueranteile je Steuersatz ausweisen

## 5. Datenhaltung

Die Daten werden in SQLite gespeichert. Standardpfad:

```text
data/kassensystem.db
```

JavaFX und Spring Boot nutzen standardmaessig dieselbe Datei. Alternativ kann der Pfad gesetzt werden ueber:

```text
-Dkassensystem.db.path=<pfad>
KASSENSYSTEM_DB_PATH=<pfad>
```

## 6. Benutzungsoberflaechen

### JavaFX

Die JavaFX-Anwendung startet mit einer Anmeldung. Kassierer werden direkt in die Kasse geleitet. Admins sehen das Hauptmenue und koennen die Verwaltung oeffnen.

Testnutzer:

```text
admin / 1234
kassierer / 1234
```

### Spring-Boot-Adminbereich

Der Adminbereich ist erreichbar unter:

```text
http://localhost:8080/kassensystem/admin/
```

Er bietet Produktliste, Filter, Produktformular und Warenzugang. Der Zugriff auf `/admin/**` und `/api/**` ist serverseitig mit Spring Security geschuetzt. Fuer den Browser gilt ebenfalls:

```text
admin / 1234
```

## 7. Fehlerbehandlung

- Leerer Warenkorb verhindert Kaufabschluss.
- Nicht vorhandene Produkte werden gemeldet.
- Negative oder leere Mengen werden abgewiesen.
- Preise muessen groesser als 0 sein.
- Lagerbestand darf nicht negativ werden.
- Zu geringer Lagerbestand verhindert den Verkauf.

## 8. Technische Umsetzung

| Bereich | Umsetzung |
|---|---|
| Sprache | Java |
| Version | Java 24 im aktuellen Projekt |
| Desktop-GUI | JavaFX |
| Backend | Spring Boot |
| Datenbank | SQLite |
| Build | Maven Multi-Module |
| Tests | JUnit |

Hinweis zur Vorgabe "rein prozedural": Das vorhandene Projekt wurde bereits als objektorientierte JavaFX-Anwendung umgesetzt. Diese Architektur erfuellt die fachlichen Muss-Anforderungen und trennt UI, Fachlogik und Datenzugriff sauber. Eine strikt prozedurale Konsolenversion waere ein alternatives Abgabeformat, ist aber nicht die aktuell gewaehlte Projektarchitektur.

## 8.1 Abgleich mit dem urspruenglichen Lastenheft

| Punkt | Stand |
|---|---|
| Hauptmenue | Erfuellt ueber Admin-Hauptmenue nach Login |
| Kassierer nur Kasse | Erfuellt ueber Rolle `KASSIERER` |
| Admin darf verwalten | Erfuellt ueber Rolle `ADMIN` |
| Browser ohne Passwort | Behoben durch Spring Security |
| Rein prozedurale Programmierung | Bewusste Abweichung, da grafische OOP-Architektur gewaehlt |

## 9. Abnahmekriterien

- JavaFX startet ohne Fehler.
- Spring Boot startet mit Java 24.
- Produktliste wird angezeigt.
- Produkt kann in den Warenkorb gelegt werden.
- Kaufabschluss erzeugt Bon und senkt Lagerbestand.
- Warenzugang erhoeht Lagerbestand.
- Admin-Weboberflaeche liest und schreibt dieselbe SQLite-Datenbank.
- Tests laufen erfolgreich.

## 10. Offene Punkte

Die fachlichen Mindestanforderungen des Lastenhefts sind umgesetzt. Offene optionale Aufgaben stehen in `TODO.md`.
