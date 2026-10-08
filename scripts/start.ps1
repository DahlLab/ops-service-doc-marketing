# =============================================================================
# start.ps1 - startet OpsServiceDoc komplett mit einem Aufruf (Windows)
#   1. Docker Desktop (falls noch nicht an) und Docker Compose (MongoDB, GLPI)
#   2. Backend (Spring Boot, Port 8080) in eigenem Fenster
#   3. Frontend (Vite, Port 5173) in eigenem Fenster
#   4. Browser mit der App oeffnen, sobald alles bereit ist
# Aufruf: Doppelklick auf start.bat (ruft dieses Skript auf)
# =============================================================================

$ErrorActionPreference = 'Stop'

# Projektstamm = Ordner ueber "scripts"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# ---------- 1. Docker ----------
# "docker info" schlaegt fehl, solange Docker Desktop nicht laeuft.
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Docker laeuft noch nicht - ich starte Docker Desktop ...'
    Start-Process "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe"
    # Bis zu 3 Minuten warten, bis der Docker-Dienst antwortet
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 2
        docker info *> $null
        if ($LASTEXITCODE -eq 0) { break }
    }
    if ($LASTEXITCODE -ne 0) { throw 'Docker ist nach 3 Minuten nicht bereit. Bitte Docker Desktop pruefen.' }
}
Write-Host 'Starte MongoDB und GLPI ...'
docker compose up -d

# ---------- Umgebungsvariablen aus der .env laden ----------
# Das Backend braucht sie als Umgebungsvariablen (GitHub-Login, GLPI-Tokens ...).
# Prozesse, die ich unten starte, erben sie von diesem Skript.
if (Test-Path "$root\.env") {
    Get-Content "$root\.env" | ForEach-Object {
        # Zeilen "NAME=Wert"; Kommentare (#) und Leerzeilen ueberspringe ich
        if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)\s*$') {
            Set-Item -Path "Env:$($Matches[1])" -Value $Matches[2].Trim('"')
        }
    }
} else {
    Write-Warning '.env fehlt im Projektstamm - Login und GLPI-Sync funktionieren dann nicht.'
}

# Prueft, ob auf einem Port schon etwas lauscht (dann nicht doppelt starten)
function Test-Port($port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

# ---------- 2. Backend ----------
if (-not (Test-Port 8080)) {
    Write-Host 'Starte Backend ...'
    Start-Process cmd.exe -ArgumentList '/k', 'title OpsServiceDoc Backend && mvnw.cmd spring-boot:run' -WorkingDirectory "$root\backend"
} else { Write-Host 'Backend laeuft schon (Port 8080).' }

# ---------- 3. Frontend ----------
if (-not (Test-Port 5173)) {
    Write-Host 'Starte Frontend ...'
    Start-Process cmd.exe -ArgumentList '/k', 'title OpsServiceDoc Frontend && npm run dev' -WorkingDirectory "$root\frontend"
} else { Write-Host 'Frontend laeuft schon (Port 5173).' }

# ---------- 4. Warten und Browser oeffnen ----------
Write-Host 'Warte, bis Backend und Frontend bereit sind ...'
for ($i = 0; $i -lt 90; $i++) {
    if ((Test-Port 8080) -and (Test-Port 5173)) { break }
    Start-Sleep -Seconds 2
}
Start-Process 'http://localhost:5173'
Write-Host 'Fertig: http://localhost:5173  (GLPI: http://localhost)'
