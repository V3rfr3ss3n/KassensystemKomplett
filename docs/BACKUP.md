# Daten sichern und wiederherstellen

Benutzer, Produkte und Bons liegen in `kassensystem.db`. Hochgeladene Produktbilder liegen im benachbarten Ordner `images`. Beides gehört zu einer vollständigen Sicherung. Vor Kopieren oder Wiederherstellen das Backend und die JavaFX-App schließen; so wird keine laufende SQLite-Transaktion kopiert. Die folgenden Befehle gelten für PowerShell im Projektordner.

## Lokaler Maven-Start

Nach dem Beenden beider Anwendungen:

```powershell
$backupDir = Join-Path (Get-Location) ('backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
Copy-Item -LiteralPath .\data -Destination $backupDir -Recurse
Test-Path (Join-Path $backupDir 'kassensystem.db')
```

Für die Wiederherstellung das Backend erneut beenden, den gewünschten Sicherungsordner einsetzen und die Sicherung prüfen:

```powershell
$backupDir = (Resolve-Path '.\backup-YYYYMMDD-HHMMSS').Path
if (-not (Test-Path (Join-Path $backupDir 'kassensystem.db'))) { throw 'Datenbank fehlt in der Sicherung.' }
New-Item -ItemType Directory -Path .\data -Force | Out-Null
Remove-Item -LiteralPath .\data\kassensystem.db-wal, .\data\kassensystem.db-shm -ErrorAction SilentlyContinue
Copy-Item -Path (Join-Path $backupDir '*') -Destination .\data -Recurse -Force
```

Danach Backend und Kasse starten und Anmeldung, Produktbild sowie einen historischen Bon prüfen. Die bisherige `data`-Datei vor einer Wiederherstellung zusätzlich aufheben, falls der falsche Sicherungsstand gewählt wurde.

## Docker Compose

Das Datenvolume enthält Datenbank und Bilder. Zum Sichern:

```powershell
$backupDir = Join-Path (Get-Location) ('backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backupDir | Out-Null
docker compose stop backend
docker compose run --rm --no-deps --user 0 --entrypoint sh -v "${backupDir}:/backup" backend -c 'cp -a /data/. /backup/'
Test-Path (Join-Path $backupDir 'kassensystem.db')
docker compose up -d backend
```

Zum Wiederherstellen den Backend-Container stoppen und einen **vorhandenen** Sicherungsordner angeben. Die folgende Kopie überschreibt Datenbank und gleichnamige Bilddateien im Volume:

```powershell
$backupDir = (Resolve-Path '.\backup-YYYYMMDD-HHMMSS').Path
if (-not (Test-Path (Join-Path $backupDir 'kassensystem.db'))) { throw 'Datenbank fehlt in der Sicherung.' }
docker compose stop backend
docker compose run --rm --no-deps --user 0 --entrypoint sh -v "${backupDir}:/backup:ro" backend -c 'test -s /backup/kassensystem.db && rm -f /data/kassensystem.db-wal /data/kassensystem.db-shm && cp -a /backup/. /data/ && chown -R 10001:0 /data'
docker compose up -d backend
```

Die Bilder gehören in denselben Sicherungsordner wie `kassensystem.db`. `docker compose down -v` löscht das Datenvolume und ist kein Sicherungsbefehl. Die Sicherung vor einem Versionswechsel an einen zweiten Ort kopieren.
