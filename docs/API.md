# Kassensystem REST-API

Basis-URL: `http://localhost:8080/kassensystem`. JSON wird mit UTF-8 übertragen. Der JavaFX-Client nutzt HTTP Basic Auth mit `X-Kassensystem-Client: JavaFX`; die Webverwaltung nutzt eine Browser-Sitzung und sendet bei Schreibzugriffen den Token aus `GET /api/session` als Header `X-CSRF-TOKEN`. Bei einer Browser-Sitzung hat diese Vorrang vor zwischengespeicherten Basic-Zugangsdaten. Benutzer und BCrypt-Passwort-Hashes liegen in SQLite. Bei leerer Benutzertabelle wird der erste Admin angelegt. Ohne gesetztes `KASSENSYSTEM_AUTH_INITIAL_ADMIN_PASSWORD` erzeugt das Backend ein zufälliges einmaliges Startpasswort und protokolliert es beim Start; beim ersten Login muss es geändert werden. Demo-Konten mit `1234` gibt es nur bei `KASSENSYSTEM_AUTH_DEMO=true` und leerer Benutzertabelle.

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

Die Tabelle zeigt die Standardrechte der Rollen. Einzelrechte können diese pro Benutzer erlauben oder verweigern; Verweigerungen haben Vorrang.

| Methode | Pfad | Recht | Zweck |
|---|---|---|---|
| GET | `/api/session` | angemeldet | Benutzer-ID, Anzeigename, Rollen, wirksame Rechte, Passwortwechselstatus und CSRF-Token |
| POST | `/api/account/password` | angemeldet | Eigenes Passwort ändern (`oldPassword`, `newPassword`); danach erneut anmelden |
| POST | `/api/auth/browser-ticket` | Verwaltungsrecht | Ein einmalig nutzbares Ticket für `GET /auth/javafx-login?ticket=...` ausstellen |
| GET, POST | `/api/admin/users` | `users.manage` | Benutzer auflisten und anlegen |
| GET, PATCH | `/api/admin/users/{id}` | `users.manage` | Benutzer lesen und ändern |
| POST | `/api/admin/users/{id}/password-reset` | `users.manage` | Startpasswort setzen; Benutzer muss es ändern |
| GET | `/api/admin/permissions` | `users.manage` | Rollen, Rechte und Rollenstandards abrufen |

`POST /api/admin/users` erwartet `username`, `displayName`, `password`, `roles` und `overrides`. `PATCH` akzeptiert `displayName`, `active`, `roles` und `overrides` auch einzeln; fehlende Felder bleiben unverändert. `overrides` ordnet Rechten `true` (erlauben) oder `false` (verweigern) zu. Die Antworten enthalten nie Passwort-Hashes. Fehlende Anmeldung liefert 401, fehlendes Recht 403 und ungültige Eingaben 400.

`POST /api/bilder` erwartet `multipart/form-data` mit dem Feld `datei` (PNG, JPEG oder GIF, höchstens 5 MB und 5000 × 5000 Pixel). Die Antwort enthält `url`, beispielsweise `api/bilder/550e8400-e29b-41d4-a716-446655440000.png`. Diesen Wert im Feld `bildPfad` des Produkts speichern. Der Server verkleinert große Bilder und legt sie neben der Datenbank im Verzeichnis `images` ab. Das Bild kann über die zurückgegebene URL ohne Anmeldung angezeigt werden, damit es auch in der JavaFX-Kasse erscheint.

Produktdaten enthalten das optionale Feld `kategorie`. Erlaubte Werte sind `Obst`, `Gemüse`, `Backwaren`, `Lebensmittel`, `Getränke`, `Elektronik`, `Haushalt`, `Hygiene` und `Sonstiges`. Andere Werte beantwortet die API mit HTTP 400. Fehlt das Feld, verwendet der Server „Sonstiges“. Beim Start ergänzt die SQLite-Migration das Feld auch bei vorhandenen Produkten. Ältere frei eingegebene Kategorien bleiben in SQLite erhalten und werden in der API als „Sonstiges“ angezeigt, bis das Produkt erneut gespeichert wird.

Produkte besitzen außerdem `scanCode`. Bei der Anlage ohne Code erzeugt das Backend nach Vergabe der ID einen stabilen Wert wie `KS-P-000123`; alte Produkte erhalten ihn bei der Migration. Ein manuell angegebener Code wird getrimmt, muss 1–128 druckbare ASCII-Zeichen enthalten und ist exakt eindeutig (Duplikat: HTTP 409). Beim Aktualisieren erhält ein fehlender oder leerer Wert den vorhandenen Code. Produktlisten und Einzelabruf liefern `scanCode` zurück.

`POST /api/produkte/etiketten` akzeptiert `{ "produktIds": [1, 2] }` und liefert einen A4-PDF-Bogen mit 24 QR-Etiketten pro Seite (`application/pdf`). Doppelte IDs ergeben nur ein Etikett; leere Auswahl wird mit HTTP 400 und unbekannte IDs mit HTTP 404 beantwortet. Der Endpunkt benötigt `products.manage` und im Browser einen CSRF-Token.

`POST /api/produkte/scan-bild` akzeptiert ein einzelnes JPEG- oder PNG-Bild (höchstens 1 MB und 4 Megapixel) und liefert bei erkanntem QR-, EAN- oder Code-128-Wert `{ "scanCode": "..." }`. Ohne erkannten Code antwortet er mit HTTP 204, bei ungültigem Bild mit HTTP 400 und bei zu großem Bild mit HTTP 413. Er benötigt `products.read` und im Browser einen CSRF-Token. Kamerabilder werden nur für die Erkennung verarbeitet und nicht gespeichert. Die Handy-Webseite benötigt HTTPS für den Kamerazugriff.

## Beispiel: Verkauf

```bash
curl -u 'kassierer:MEIN_PASSWORT' -H 'X-Kassensystem-Client: JavaFX' -H 'Content-Type: application/json' \
  -d '{"positionen":[{"produktId":1,"menge":2}]}' \
  http://localhost:8080/kassensystem/api/kasse/abschluss
```

Erfolg: HTTP 201 mit `bonnummer`, `datumUhrzeit`, `gesamtpreis` und `positionen`. Jede Position enthält `produktId`, `produktName`, `einheit`, `menge`, `einzelpreis`, `steuerSatz`, `gesamtpreis`. Der Server übernimmt Preis, Name, Einheit und Steuer aus der Datenbank. Er prüft den Bestand und speichert Lagerabbuchung und Bon in einer Transaktion. Fehlerhafte Daten liefern HTTP 400, fehlende Produkte oder Bons HTTP 404, fehlende Berechtigung HTTP 403.

```bash
curl -u 'kassierer:MEIN_PASSWORT' http://localhost:8080/kassensystem/api/bons
curl -u 'lagerist:MEIN_PASSWORT' -H 'X-Kassensystem-Client: JavaFX' -H 'Content-Type: application/json' \
  -d '{"menge":5}' http://localhost:8080/kassensystem/api/produkte/1/warenzugang
```
