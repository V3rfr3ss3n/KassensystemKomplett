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

Die JavaFX-Kasse und die Webverwaltung nutzen die geschützte REST-API. Nur das Backend schreibt in SQLite. Berechtigte Benutzer öffnen den Adminbereich aus JavaFX über ein vom Backend ausgestelltes, einmalig nutzbares Ticket.

## Rollen und Bedienung

- **Admin:** Hauptmenü, Kasse, Produktpflege und Warenzugang.
- **Kassierer:** nur Kasse, Warenkorb, Kaufabschluss und Bon-Historie.
- **Lagerist:** nur Verwaltung, Produktliste und Warenzugang.

Benutzer, Rollen und einzelne erlaubte oder verweigerte Rechte liegen in SQLite. Der erste Admin wird mit einem eigenen Startpasswort eingerichtet und muss es wechseln. Die früheren Demo-Zugänge entstehen nur bei ausdrücklich aktiviertem Entwicklungsmodus.

## Fachliche Abläufe

1. Die Kasse lädt Produkte und zeigt Suche, Filter, Mengenwahl und Warenkorb.
2. Beim Kauf werden Bestand und Menge erneut geprüft.
3. Das Backend setzt Preise und Steuer aus dem aktuellen Produktstand fest. SQLite speichert Lagerabbuchung und Bon in einer Transaktion. Bei einem Fehler bleibt der Warenkorb erhalten und die Datenbank unverändert.
4. Bonpositionen halten Produktname, Einheit, Einzelpreis und Steuersatz zum Kaufzeitpunkt fest.
5. Neue und historische Bons können angezeigt und als PDF oder TXT gespeichert werden.
6. Die Webverwaltung bietet Produktliste, Filter, Anlage, Bearbeitung, Löschung und Warenzugang nach Rolle.

## Qualität und Abnahme

- Produktname, Preis, Bestand und Mengen werden geprüft; ungültige oder nicht endliche Zahlen werden abgewiesen.
- SQLite-Fehler werden als Fehlermeldung weitergegeben, statt unbemerkt zu einem scheinbaren Erfolg zu führen.
- Tests prüfen Kassenlogik, SQL-Transaktion, Bon-Snapshots, PDF-Inhalt, API und Rollenrechte.
- Die frühere Demo-Abnahme mit allen drei Rollen, Kauf und Bons wurde am 02.10.2026 bestätigt. Die neue Benutzeroberfläche benötigt noch eine visuelle Abnahme in JavaFX-WebView und Browser.
- Sicherung und Wiederherstellung umfassen SQLite und Produktbilder; eine isolierte Docker-Probe ist erfolgreich abgeschlossen. Die Anleitung steht in [docs/BACKUP.md](docs/BACKUP.md).
- Die Startanleitung und Screenshot-Plätze stehen in der [README](README.md).

## Bewusste Grenzen der Schulabgabe

- Ein produktiver Mehrplatzbetrieb mit Transportverschlüsselung und zentralem Deployment ist noch nicht vorgesehen. Der Backend-Port ist lokal gebunden.
- Bereits gelöschte Produkte in alten Datenbanken können für historische Bons nur als Produktnummer angezeigt werden, falls zuvor kein Name gespeichert wurde.

## Restarbeiten

Die laufende Aufgabenliste steht in [TODO.md](TODO.md). Anforderungen und Abnahmekriterien stehen im [Pflichtenheft](PFLICHTENHEFT.md).
