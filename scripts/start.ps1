$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Docker is not running yet - starting Docker Desktop ...'
    Start-Process "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe"
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 2
        docker info *> $null
        if ($LASTEXITCODE -eq 0) { break }
    }
    if ($LASTEXITCODE -ne 0) { throw 'Docker is not ready after 3 minutes. Please check Docker Desktop.' }
}
Write-Host 'Starting MongoDB and GLPI ...'
docker compose up -d

if (Test-Path "$root\.env") {
    Get-Content "$root\.env" | ForEach-Object {
        if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)\s*$') {
            Set-Item -Path "Env:$($Matches[1])" -Value $Matches[2].Trim('"')
        }
    }
} else {
    Write-Warning '.env is missing in the project root - login and GLPI sync will not work.'
}

function Test-Port($port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

if (-not (Test-Port 8080)) {
    Write-Host 'Starting backend ...'
    Start-Process cmd.exe -ArgumentList '/k', 'title OpsServiceDoc Backend && mvnw.cmd spring-boot:run' -WorkingDirectory "$root\backend"
} else { Write-Host 'Backend is already running (port 8080).' }

if (-not (Test-Port 5173)) {
    Write-Host 'Starting frontend ...'
    Start-Process cmd.exe -ArgumentList '/k', 'title OpsServiceDoc Frontend && npm run dev' -WorkingDirectory "$root\frontend"
} else { Write-Host 'Frontend is already running (port 5173).' }

Write-Host 'Waiting for backend and frontend ...'
for ($i = 0; $i -lt 90; $i++) {
    if ((Test-Port 8080) -and (Test-Port 5173)) { break }
    Start-Sleep -Seconds 2
}
Start-Process 'http://localhost:5173'
Write-Host 'Ready: http://localhost:5173  (GLPI: http://localhost)'
