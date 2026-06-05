# Kassensystem

JavaFX-Kassensystem mit Spring-Boot-Adminbereich und gemeinsamer SQLite-Datenbank.

## Funktionen

- Kassenansicht mit Produktliste, Suche, Filter und Warenkorb
- Produkte per Plus-Klick in den Warenkorb legen
- Warenkorbpositionen einzeln erhoehen, verringern oder entfernen
- Kaufabschluss mit Lagerpruefung, Bonanzeige und Bon-Historie
- Produktdaten mit Preis, Bestand, Einheit, Steuer und optionalem Bild
- Gemeinsame SQLite-Datenbank fuer JavaFX und Spring Boot
- Spring-Adminbereich fuer Produkt- und Lagerverwaltung

## Starten

Voraussetzung: JDK 24 und Maven.

JavaFX-Kasse:

```bash
mvn -pl kassensystem-javafx javafx:run
```

Spring-Adminbereich:

```bash
mvn -pl kassensystem-backend spring-boot:run
```

Admin-Weboberflaeche:

```text
http://localhost:8080/kassensystem/admin/
```

JavaFX fragt fuer den Verwaltungstab ein Testpasswort ab:

```text
1234
```

## Datenbank

Standardpfad:

```text
data/kassensystem.db
```

Der Pfad kann gesetzt werden mit:

```text
-Dkassensystem.db.path=<pfad>
KASSENSYSTEM_DB_PATH=<pfad>
```

## Projektstruktur

- `kassensystem-javafx` - JavaFX-Kasse
- `kassensystem-backend` - Spring-Boot-API und Admin-Weboberflaeche
- `PFLICHTENHEFT.md` - Pflichtenheft zur Aufgabe
- `TODO.md` - Restarbeiten und optionale Erweiterungen

## Tests

```bash
mvn -pl kassensystem-javafx test
mvn -pl kassensystem-backend -DskipTests package
```
