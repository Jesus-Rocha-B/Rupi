$ErrorActionPreference = 'Stop'
$envFile = "$PSScriptRoot\..\.local\env.ps1"
if (Test-Path $envFile) {
    . $envFile
}
if (!$env:RUPI_MONITOR_TOKEN) {
    $moderationTokenFile = "$PSScriptRoot\..\.local\monitor.token"
    if (Test-Path -LiteralPath $moderationTokenFile) {
        $env:RUPI_MONITOR_TOKEN = (Get-Content -LiteralPath $moderationTokenFile -Raw).Trim()
    }
}

$serviceDir = "$PSScriptRoot\..\backend\service-ia"
Set-Location $serviceDir

Write-Host "Iniciando Microservicio de IA Rupi en http://127.0.0.1:8000..." -ForegroundColor Cyan

python -m uvicorn main:app --host 127.0.0.1 --port 8000
