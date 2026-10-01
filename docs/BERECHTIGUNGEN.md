# Plan: Benutzer und Berechtigungen

## Ziel

Admins verwalten Benutzer, Zuständigkeiten und Zugänge in der Webverwaltung. Ein Benutzer erhält eine oder mehrere Rollen und bei Bedarf einzelne zusätzliche oder entzogene Rechte. Die wirksamen Rechte sind in JavaFX und Browser gleich und werden **bei jeder API-Anfrage vom Backend** geprüft. Die Oberfläche blendet unzulässige Aktionen zusätzlich aus.

## Ausgangslage

- Derzeit existieren drei fest kodierte Demo-Benutzer in JavaFX und im Spring-Backend. Beide Listen müssen synchron bleiben.
- Das Backend liefert in `GET /api/session` nur `manageProducts` und `bookStock`. JavaFX entscheidet noch anhand einer einzigen lokalen Rolle.
- SQLite speichert Produkte, Bestände und Bons, aber keine Benutzer oder Rechte.
- Die Browser-Anmeldung und das JavaFX-SSO nutzen eine Sitzung; der JavaFX-API-Client nutzt Basic Auth. Bei einer bestehenden Browser-Sitzung muss diese Vorrang vor vom Browser zwischengespeicherten Basic-Zugangsdaten haben.

## Rechtemodell

| Recht | Bedeutung | Admin | Kassierer | Lagerist |
|---|---|---:|---:|---:|
| `products.read` | Produktliste und Bestand sehen | ja | ja | ja |
| `products.manage` | Produkte und Bilder anlegen, ändern, löschen | ja | nein | nein |
| `stock.book` | Warenzugang buchen | ja | nein | ja |
| `sales.create` | Kauf abschließen | ja | ja | nein |
| `receipts.read` | Bon-Historie ansehen und exportieren | ja | ja | nein |
| `users.manage` | Benutzer und Rechte verwalten | ja | nein | nein |

Rollen enthalten Standardrechte. Ein Admin kann pro Benutzer zusätzliche Rechte erlauben oder verweigern; eine ausdrückliche Verweigerung hat Vorrang vor einer Rollenerlaubnis. Die Bearbeitungsmaske zeigt sowohl Rollen als auch die daraus berechneten wirksamen Rechte. Neue Rechte werden zentral im Backend definiert, damit alte Clients unbekannte Rechte nicht versehentlich gewähren.

## Umsetzung in Etappen

1. **Daten und Migration:** SQLite-Tabellen für Benutzer (eindeutiger normalisierter Login, Passwort-Hash, aktiv, Zeitstempel), Rollen, Rollenzuordnung, Rollenrechte und benutzerspezifische Erlaubnisse/Verweigerungen anlegen. Bestehende Kassendaten bleiben erhalten. Demo-Konten nur im Entwicklungsprofil befüllen; für eine bereitgestellte Instanz den ersten Admin über eine einmalige Einrichtung mit Kennwort aus einer Umgebungsvariable anlegen.
2. **Einheitliche Anmeldung:** JavaFX-`AuthService` von fest kodierten Nutzern auf die Backend-Anmeldung umstellen. Das Backend liefert Benutzer-ID, Anzeigenamen, Rollen und wirksame Rechte an beide Oberflächen. API-Client und SSO dürfen keine abweichenden lokalen Rollenzuordnungen mehr verwenden. Passwort-Hashes mit einem adaptiven Algorithmus speichern; Passwörter niemals an die Benutzerliste zurückgeben.
3. **Serverseitige Regeln:** Endpunkte für Produktpflege, Warenzugang, Verkauf, Bons und Benutzerverwaltung anhand der wirksamen Rechte absichern. Änderungen an Rechten wirken spätestens bei der nächsten Anfrage; bestehende Sitzungen nach Deaktivierung, Passwortwechsel und Rollenänderung widerrufen oder neu prüfen. Für Browser-Schreibzugriffe CSRF-Schutz aktivieren. Berechtigungsänderungen mit ausführendem Admin und Zeitpunkt protokollieren.
4. **Admin-Oberfläche:** Bereich „Benutzer & Rechte“ mit Suche, Benutzeranlage, Aktivieren/Deaktivieren, Rollenwahl, einzelnen Rechte-Overrides und Passwort-Zurücksetzen. Vor dem Speichern die wirksamen Rechte anzeigen und Änderungen verständlich bestätigen. Den letzten aktiven Benutzer mit `users.manage` weder deaktivieren noch sich selbst dieses Recht entziehen lassen.
5. **API und Dokumentation:** `GET/POST /api/admin/users`, `GET/PATCH /api/admin/users/{id}`, `POST /api/admin/users/{id}/password-reset`, `GET /api/admin/permissions` dokumentieren. Antworten enthalten keine Passwort-Hashes. Fehlende Anmeldung ergibt 401, fehlendes Recht 403, ungültige Eingaben 400.

## Abnahme

- Admin legt einen Benutzer an, weist Rollen und Einzelrechte zu, ändert und sperrt dessen Zugang; Browser und JavaFX zeigen danach dieselben Funktionen.
- Lagerist sieht in beiden Oberflächen Bestand und Warenzugang, aber keine Produktpflege; direkte Produkt-Schreibaufrufe liefern 403.
- Kassierer erreicht Verkauf und Bons, aber keine Verwaltung.
- Ein entzogenes Einzelrecht überschreibt eine Rollenerlaubnis. Der letzte berechtigte Admin kann nicht ausgesperrt werden.
- Migration erhält bestehende Produkte und Bons. Nach Neustart bleiben Benutzer und Rechte erhalten.
- Tests decken Rollen, Overrides, Sitzungswechsel, deaktivierte Benutzer, Passwortwechsel, 401/403 und die Schutzregel für den letzten Admin ab.

Die Umsetzung dieses Plans ist eine eigene Erweiterung nach der aktuellen Demo-Version.
