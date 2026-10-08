$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

foreach ($port in 8080, 5173) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}
docker compose stop
Write-Host 'Everything stopped.'
