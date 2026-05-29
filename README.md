# Kassensystem MVP

Dieses Projekt ist ein kleines JavaFX-Kassensystem mit Maven. Die aktuelle Version enthält die wichtigsten MVP-Funktionen für den Verkauf, die Lagerverwaltung und die Produktanlage.

## Funktionen

- Produktübersicht mit Gesamtbestand und Lagerwert
- Kassenansicht mit Warenkorb und Bonvorschau
- Lageransicht mit Warenzugang
- Produkt anlegen mit Eingabeprüfung
- einfache JavaFX-Oberfläche mit CSS-Styling

## Starten

Mit JDK 24 und Maven:

```bash
mvn clean compile
mvn clean javafx:run
```

Falls das Projekt in VS Code gestartet wird, sollte die Java-Umgebung auf JDK 24 zeigen.

## Projektstruktur

- src/main/java/de/mmbbs/kassensystem/Main.java – Einstiegspunkt der JavaFX-Anwendung
- src/main/java/de/mmbbs/kassensystem/ui/ – JavaFX-Ansichten für Start, Kasse, Lager und Produktanlage
- src/main/java/de/mmbbs/kassensystem/service/ – Geschäftslogik für Produkte, Kassenvorgänge und Bons
- src/main/java/de/mmbbs/kassensystem/model/ – Datenmodelle wie Produkt, Bon und BonPosition
- src/main/resources/styles.css – JavaFX-Styling

## Hinweis

Die Datenhaltung ist aktuell nur im Arbeitsspeicher umgesetzt. Für spätere Erweiterungen sind JSON oder SQLite als nächste Schritte gedacht.
