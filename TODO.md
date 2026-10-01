# TODO / Restarbeiten

## Pflichtanforderungen aus dem Lastenheft

- [x] Produktverwaltung mit Name, Preis und Lagerbestand
- [x] Kassenvorgang mit Produktwahl und Menge
- [x] Lagerpruefung vor Verkauf
- [x] Gesamtpreisberechnung
- [x] Bon-Ausgabe auf dem Bildschirm
- [x] Lagerbestand nach Verkauf aktualisieren
- [x] Warenzugang erfassen
- [x] Lagerbestand anzeigen
- [x] Fehlerhafte Eingaben abfangen
- [x] Pflichtenheft erstellen
- [x] Code mit erklaerenden Javadocs ergaenzen
- [x] Hauptmenue fuer Admins ergaenzen
- [x] Kassiererrolle direkt auf Kasse begrenzen
- [x] Browser-Adminbereich serverseitig absichern
- [x] Lageristenrolle fuer Warenzugang ohne Kassenzugriff ergaenzen
- [x] JavaFX-SSO in die Webverwaltung ohne zweite Anmeldung ergaenzen
- [x] Darkmode fuer JavaFX und Browserverwaltung ergaenzen

## Bewusste Abweichung

- [x] Objektorientierte JavaFX-/Spring-Architektur als gueltige Abgabeentscheidung dokumentiert; laut Projektvorgabe ist die Technologiewahl frei.

## Optionale Verbesserungen

- [x] Serverseitigen Admin-Login mit Spring Security statt nur JavaFX-Testpasswort umsetzen.
- [x] Rollenrechte serverseitig trennen: Admin Produktpflege, Lagerist Warenzugang.
- [x] API-Integrationstests fuer Produktanlage, Liste, Warenzugang, Bearbeitung und Loeschung ergaenzen.
- [x] Ungueltige Zahlenwerte und ungueltige Warenzugangsmenge in der API abweisen.
- [x] Lageruebersicht mit Gesamtzahl, verfuegbaren und nicht verfuegbaren Produkten ergaenzen.
- [x] README mit Startanleitung, Bedienung und Screenshot-Plaetzen erweitern.
- [x] PDF- und TXT-Speicherung fuer aktuelle und historische Bons ergaenzen.
- [x] Bon und Lagerabbuchung in einer SQLite-Transaktion speichern.
- [x] Produktname und Einheit im Bon als Snapshot erhalten.
- [x] SQLite-Speicherfehler in der Kasse sichtbar weitergeben.
- [x] Windows-Startskript fuer Backend und JavaFX ergaenzen.
- [ ] Manuelle GUI-Abnahme mit allen drei Rollen, PDF- und TXT-Speicherung sowie Produktauswahl auf einem Desktop durchfuehren ([GitHub-Issue #1](https://github.com/V3rfr3ss3n/KassensystemKomplett/issues/1)).
- [x] Admin-Weboberflaeche optisch mit Darkmode verfeinern.
- [x] Produktliste in der Webverwaltung per Tastatur bedienbar machen.
- [x] JavaFX-Kasse auf REST-API für Produkte, Verkäufe und Bon-Historie umstellen.
- [x] Kaufabschluss im Backend transaktional mit serverseitigen Preisen umsetzen.
- [x] Docker-Image und Compose für das Backend ergänzen.
- [x] CI-Quality-Gate mit Tests, Coverage, JavaScript-Prüfung und Docker-Build ergänzen.
- [x] Release-Workflow für Windows-App und GitHub Container Registry ergänzen.
- [x] Docker-Build im GitHub-Quality-Gate und Release-Workflow mit Windows-ZIP und GHCR-Image prüfen.
- [x] Docker Compose lokal starten; API-Rollen und einen Testkauf mit Bon-Snapshot in einem fluechtigen Container pruefen.
- [ ] JavaFX-Kasse manuell gegen den laufenden Docker-Container bedienen und GUI-Abnahme abschliessen.
