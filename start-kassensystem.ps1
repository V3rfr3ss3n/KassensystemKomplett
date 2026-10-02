[CmdletBinding()]
param([switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
$projektOrdner = $PSScriptRoot
$javaKandidaten = @()
if ($env:JAVA_HOME) { $javaKandidaten += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
$javaKandidaten += 'C:\Program Files\Java\jdk-24\bin\java.exe'
$javaBefehl = Get-Command java.exe -ErrorAction SilentlyContinue
if ($javaBefehl) { $javaKandidaten += $javaBefehl.Source }
$javaPfad = $javaKandidaten | Where-Object { Test-Path $_ } | Where-Object {
    (Get-Item $_).VersionInfo.ProductVersion -match '^24(\.|$)'
} | Select-Object -First 1
if (-not $javaPfad) {
    Write-Host 'JDK 24 wurde nicht gefunden. Bitte JDK 24 installieren oder JAVA_HOME darauf setzen.' -ForegroundColor Red
    exit 1
}
$env:JAVA_HOME = Split-Path (Split-Path $javaPfad -Parent) -Parent
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH

$mavenBefehl = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$mavenPfad = if ($mavenBefehl) { $mavenBefehl.Source } else { 'C:\Program Files\Apache\apache-maven-3.9.10\bin\mvn.cmd' }
if (-not (Test-Path $mavenPfad)) {
    Write-Host 'Maven wurde nicht gefunden. Bitte Maven installieren und mvn.cmd in PATH aufnehmen.' -ForegroundColor Red
    exit 1
}
if ($CheckOnly) {
    Write-Host "JDK 24: $javaPfad"
    Write-Host "Maven: $mavenPfad"
    exit 0
}

Set-Location $projektOrdner
$protokoll = Join-Path $projektOrdner 'data\backend-start.log'
New-Item -ItemType Directory -Path (Split-Path $protokoll -Parent) -Force | Out-Null
function Test-VerwaltungErreichbar {
    $verbindung = [System.Net.Sockets.TcpClient]::new()
    try {
        $verbindung.Connect('127.0.0.1', 8080)
        return $true
    } catch {
        return $false
    } finally {
        $verbindung.Dispose()
    }
}
if (Test-VerwaltungErreichbar) {
    Write-Host 'Port 8080 ist bereits belegt. Bitte die laufende Verwaltung zuerst beenden.' -ForegroundColor Red
    exit 1
}
Write-Host 'Starte Spring-Verwaltung im Hintergrund. Protokoll:' $protokoll
$backendJob = Start-Job -ArgumentList $mavenPfad, $projektOrdner, $protokoll, $env:JAVA_HOME, $env:KASSENSYSTEM_AUTH_INITIAL_ADMIN_PASSWORD, $env:KASSENSYSTEM_AUTH_DEMO -ScriptBlock {
    param($maven, $ordner, $logdatei, $jdk, $initialAdminPassword, $demoMode)
    $env:JAVA_HOME = $jdk
    $env:KASSENSYSTEM_AUTH_INITIAL_ADMIN_PASSWORD = $initialAdminPassword
    $env:KASSENSYSTEM_AUTH_DEMO = $demoMode
    $env:PATH = (Join-Path $jdk 'bin') + ';' + $env:PATH
    Set-Location $ordner
    & $maven -pl kassensystem-backend spring-boot:run *> $logdatei
}
$kassenExitCode = 1
try {
    $bereit = $false
    for ($versuch = 0; $versuch -lt 45; $versuch++) {
        if (Test-VerwaltungErreichbar) {
            $bereit = $true
            break
        }
        if ($backendJob.State -ne 'Running') {
            break
        }
        Start-Sleep -Seconds 1
    }
    if (-not $bereit) {
        throw "Die Verwaltung konnte nicht gestartet werden. Bei der ersten Einrichtung KASSENSYSTEM_AUTH_INITIAL_ADMIN_PASSWORD setzen. Details: $protokoll"
    }
    Write-Host 'Starte JavaFX-Kasse. Beim Schliessen der Kasse wird auch die Verwaltung beendet.'
    & $mavenPfad -pl kassensystem-javafx javafx:run
    $kassenExitCode = $LASTEXITCODE
} finally {
    Stop-Job $backendJob -ErrorAction SilentlyContinue
    Remove-Job $backendJob -Force -ErrorAction SilentlyContinue
}
exit $kassenExitCode
