# Benutzer und Berechtigungen

## Ziel

Admins verwalten Benutzer, Zuständigkeiten und Zugänge in der Webverwaltung. Ein Benutzer erhält eine oder mehrere Rollen und bei Bedarf einzelne zusätzliche oder entzogene Rechte. Die wirksamen Rechte sind in JavaFX und Browser gleich und werden **bei jeder API-Anfrage vom Backend** geprüft. Die Oberfläche blendet unzulässige Aktionen zusätzlich aus.

## Architektur

- SQLite speichert Benutzer, BCrypt-Passwort-Hashes, Rollenzuordnungen, Einzelrechte und Änderungsprotokolle neben Produkten und Bons.
- `GET /api/session` liefert die wirksamen Rechte. JavaFX und Browser nutzen diese Angaben; die API prüft die Rechte unabhängig von der Oberfläche.
- Browser verwenden Sitzungen und CSRF-Token. JavaFX nutzt Basic Auth lokal und erhält Browser-Einmaltickets vom Backend. Bestehende Browser-Sitzungen haben Vorrang vor zwischengespeicherten Basic-Zugangsdaten.

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

## Umsetzung

1. Beim Start werden fehlende Benutzertabellen ergänzt. Ein leerer Bestand erhält genau einen Admin mit Startpasswort aus `KASSENSYSTEM_AUTH_INITIAL_ADMIN_PASSWORD`; `KASSENSYSTEM_AUTH_DEMO=true` erzeugt stattdessen drei Entwicklungsnutzer. Rollenstandards liegen zentral im Backend, Zuordnungen und Einzelrechte in SQLite.
2. JavaFX meldet sich beim Backend an. Einmalige Browser-Tickets werden vom Server ausgestellt und nach 60 Sekunden oder beim ersten Gebrauch ungültig. Passwort-Hashes verlassen den Server nicht.
3. Die API prüft wirksame Rechte pro Anfrage. Bei Kontosperre, Rollenänderung und Passwortwechsel verlieren bestehende Browser-Sitzungen ihre Gültigkeit. Schreibzugriffe aus dem Browser benötigen CSRF-Token. Änderungen an Benutzern werden mit Admin und Zeitpunkt protokolliert.
4. Die Webverwaltung bietet Suche, Benutzeranlage, Aktivieren/Sperren, Rollen, Einzelrechte und Passwort-Zurücksetzen. Der letzte aktive Benutzer mit `users.manage` kann nicht ausgesperrt werden.
5. Die Schnittstellen sind in [API.md](API.md) dokumentiert.

## Abnahme

- Admin legt einen Benutzer an, weist Rollen und Einzelrechte zu, ändert und sperrt dessen Zugang; Browser und JavaFX zeigen danach dieselben Funktionen.
- Lagerist sieht in beiden Oberflächen Bestand und Warenzugang, aber keine Produktpflege; direkte Produkt-Schreibaufrufe liefern 403.
- Kassierer erreicht Verkauf und Bons, aber keine Verwaltung.
- Ein entzogenes Einzelrecht überschreibt eine Rollenerlaubnis. Der letzte berechtigte Admin kann nicht ausgesperrt werden.
- Migration erhält bestehende Produkte und Bons. Nach Neustart bleiben Benutzer und Rechte erhalten.
- Tests decken Rollen, Overrides, Sitzungswechsel, deaktivierte Benutzer, Passwortwechsel, 401/403 und die Schutzregel für den letzten Admin ab.

Die frühere Demo-Version hatte fest kodierte Benutzer. Bestehende Kassenbestände und Bons bleiben beim Umstieg erhalten; die Benutzer werden bei der ersten Installation der neuen Version eingerichtet.
