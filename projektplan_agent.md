# Projektplan und aktueller Stand

## Ziel

Das Kassensystem bildet die wichtigsten Abläufe eines kleinen Ladens ab: Produkte pflegen, Lagerbestand führen, Verkäufe kassieren und Bons ausgeben. Die Schulabgabe nutzt Java 24, Maven, JavaFX, Spring Boot und SQLite. Die Lehrkraft hat die freie Technologiewahl bestätigt; die Umsetzung ist objektorientiert.

## Architektur

| Bereich | Umsetzung |
|---|---|
| Kasse | JavaFX-Desktopanwendung mit Rollenlogin |
| Verwaltung | Spring-Boot-Backend mit HTML/CSS/JavaScript-Oberfläche |
| Datenhaltung | SQLite im Backend; JavaFX nutzt die REST-API |
| Build | Maven mit zwei Modulen |
| Tests | JUnit, Spring MockMvc und JaCoCo-Quality-Gate |
| Bereitstellung | Docker Compose, GitHub Actions, Windows-App als Release-ZIP |

Die JavaFX-Kasse und die Webverwaltung nutzen die geschützte REST-API. Nur das Backend schreibt in SQLite. Admin und Lagerist können den Adminbereich aus JavaFX über ein zeitlich begrenztes SSO-Ticket öffnen.

## Rollen und Bedienung

- **Admin:** Hauptmenü, Kasse, Produktpflege und Warenzugang.
- **Kassierer:** nur Kasse, Warenkorb, Kaufabschluss und Bon-Historie.
- **Lagerist:** nur Verwaltung, Produktliste und Warenzugang.

Demo-Zugänge: `admin / 1234`, `kassierer / 1234`, `lagerist / 1234`.

## Fachliche Abläufe

1. Die Kasse lädt Produkte und zeigt Suche, Filter, Mengenwahl und Warenkorb.
2. Beim Kauf werden Bestand und Menge erneut geprüft.
3. Das Backend setzt Preise und Steuer aus dem aktuellen Produktstand fest. SQLite speichert Lagerabbuchung und Bon in einer Transaktion. Bei einem Fehler bleibt der Warenkorb erhalten und die Datenbank unverändert.
4. Bonpositionen halten Produktname, Einheit, Einzelpreis und Steuersatz zum Kaufzeitpunkt fest.
5. Neue und historische Bons können angezeigt, als PDF gespeichert und gedruckt werden.
6. Die Webverwaltung bietet Produktliste, Filter, Anlage, Bearbeitung, Löschung und Warenzugang nach Rolle.

## Qualität und Abnahme

- Produktname, Preis, Bestand und Mengen werden geprüft; ungültige oder nicht endliche Zahlen werden abgewiesen.
- SQLite-Fehler werden als Fehlermeldung weitergegeben, statt unbemerkt zu einem scheinbaren Erfolg zu führen.
- Tests prüfen Kassenlogik, SQL-Transaktion, Bon-Snapshots, PDF-Inhalt, API und Rollenrechte.
- Manuell prüfen: Anmeldung mit allen drei Rollen, Produktanlage, Warenzugang, Kaufabschluss, Bestandsänderung, Neustart, historische Bons, PDF und Druckdialog.
- Die Startanleitung und Screenshot-Plätze stehen in der [README](README.md).

## Bewusste Grenzen der Schulabgabe

- Die drei fest hinterlegten Demo-Zugänge und das lokale SSO-Geheimnis sind für die Vorführung gedacht.
- Es gibt noch keinen produktiven Mehrplatzbetrieb oder Benutzerverwaltung. Die Erweiterung ist im [Berechtigungsplan](docs/BERECHTIGUNGEN.md) beschrieben.
- Bereits gelöschte Produkte in alten Datenbanken können für historische Bons nur als Produktnummer angezeigt werden, falls zuvor kein Name gespeichert wurde.

## Restarbeiten

Die laufende Aufgabenliste steht in [TODO.md](TODO.md). Anforderungen und Abnahmekriterien stehen im [Pflichtenheft](PFLICHTENHEFT.md).
