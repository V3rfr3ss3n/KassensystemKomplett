# Kassensystem REST-API

Basis-URL: `http://localhost:8080/kassensystem`. JSON wird mit UTF-8 übertragen. Der JavaFX-Client nutzt HTTP Basic Auth; die Webverwaltung nutzt eine Browser-Sitzung. Bei einer vorhandenen Browser-Sitzung hat diese Vorrang vor vom Browser zwischengespeicherten Basic-Zugangsdaten. Für die Schul-Demo gelten `admin`, `kassierer`, `lagerist` mit Passwort `1234`. Zugangsdaten sind fest im Code hinterlegt; den Demo-Container nur lokal betreiben.

| Methode | Pfad | Admin | Kassierer | Lagerist |
|---|---|---:|---:|---:|
| GET | `/api/session` | ja | ja | ja |
| GET | `/api/produkte`, `/api/produkte/{id}` | ja | ja | ja |
| POST, PUT, DELETE | `/api/produkte` bzw. `/api/produkte/{id}` | ja | nein | nein |
| POST | `/api/produkte/{id}/warenzugang` | ja | nein | ja |
| POST | `/api/kasse/abschluss` | ja | ja | nein |
| GET | `/api/bons`, `/api/bons/{id}` | ja | ja | nein |
| POST | `/api/bilder` | ja | nein | nein |
| GET | `/api/bilder/{dateiname}` | ja | ja | ja |

`POST /api/bilder` erwartet `multipart/form-data` mit dem Feld `datei` (PNG, JPEG oder GIF, höchstens 5 MB und 5000 × 5000 Pixel). Die Antwort enthält `url`, beispielsweise `api/bilder/550e8400-e29b-41d4-a716-446655440000.png`. Diesen Wert im Feld `bildPfad` des Produkts speichern. Der Server verkleinert große Bilder und legt sie neben der Datenbank im Verzeichnis `images` ab. Das Bild kann über die zurückgegebene URL ohne Anmeldung angezeigt werden, damit es auch in der JavaFX-Kasse erscheint.

Produktdaten enthalten das optionale Feld `kategorie`. Erlaubte Werte sind `Obst`, `Gemüse`, `Backwaren`, `Lebensmittel`, `Getränke`, `Elektronik`, `Haushalt`, `Hygiene` und `Sonstiges`. Andere Werte beantwortet die API mit HTTP 400. Fehlt das Feld, verwendet der Server „Sonstiges“. Beim Start ergänzt die SQLite-Migration das Feld auch bei vorhandenen Produkten. Ältere frei eingegebene Kategorien bleiben in SQLite erhalten und werden in der API als „Sonstiges“ angezeigt, bis das Produkt erneut gespeichert wird.

## Beispiel: Verkauf

```bash
curl -u kassierer:1234 -H 'Content-Type: application/json' \
  -d '{"positionen":[{"produktId":1,"menge":2}]}' \
  http://localhost:8080/kassensystem/api/kasse/abschluss
```

Erfolg: HTTP 201 mit `bonnummer`, `datumUhrzeit`, `gesamtpreis` und `positionen`. Jede Position enthält `produktId`, `produktName`, `einheit`, `menge`, `einzelpreis`, `steuerSatz`, `gesamtpreis`. Der Server übernimmt Preis, Name, Einheit und Steuer aus der Datenbank. Er prüft den Bestand und speichert Lagerabbuchung und Bon in einer Transaktion. Fehlerhafte Daten liefern HTTP 400, fehlende Produkte oder Bons HTTP 404, fehlende Berechtigung HTTP 403.

```bash
curl -u kassierer:1234 http://localhost:8080/kassensystem/api/bons
curl -u lagerist:1234 -H 'Content-Type: application/json' \
  -d '{"menge":5}' http://localhost:8080/kassensystem/api/produkte/1/warenzugang
```
