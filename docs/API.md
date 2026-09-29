# Kassensystem REST-API

Basis-URL: `http://localhost:8080/kassensystem`. JSON wird mit UTF-8 übertragen. Die API verlangt HTTP Basic Auth; die Webverwaltung verwendet zusätzlich eine Formularsitzung. Für die Schul-Demo gelten `admin`, `kassierer`, `lagerist` mit Passwort `1234`. Zugangsdaten sind fest im Code hinterlegt; den Demo-Container nur lokal betreiben.

| Methode | Pfad | Admin | Kassierer | Lagerist |
|---|---|---:|---:|---:|
| GET | `/api/session` | ja | ja | ja |
| GET | `/api/produkte`, `/api/produkte/{id}` | ja | ja | ja |
| POST, PUT, DELETE | `/api/produkte` bzw. `/api/produkte/{id}` | ja | nein | nein |
| POST | `/api/produkte/{id}/warenzugang` | ja | nein | ja |
| POST | `/api/kasse/abschluss` | ja | ja | nein |
| GET | `/api/bons`, `/api/bons/{id}` | ja | ja | nein |

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
