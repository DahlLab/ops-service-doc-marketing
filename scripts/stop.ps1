# =============================================================================
# stop.ps1 - beendet Backend, Frontend und die Docker-Container (Windows)
# Die Daten bleiben erhalten (Docker-Volumes werden NICHT geloescht).
# =============================================================================
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# Prozesse beenden, die auf den App-Ports lauschen (Backend 8080, Frontend 5173)
foreach ($port in 8080, 5173) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}
# Container anhalten (nicht entfernen)
docker compose stop
Write-Host 'Alles gestoppt.'
