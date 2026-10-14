$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginRes = Invoke-RestMethod -Uri 'http://127.0.0.1:5173/api/v1/auth/login' `
    -Method Post `
    -ContentType 'application/json' `
    -Body '{"username":"estudiante.demo","password":"123456"}' `
    -WebSession $session `
    -Headers @{ Origin = 'http://localhost:5173' }

Write-Host "Sesion estudiante iniciada: $($loginRes.name)" -ForegroundColor Yellow

$routes = Invoke-RestMethod -Uri 'http://127.0.0.1:5173/api/v1/student/learning-routes' `
    -WebSession $session `
    -Headers @{ Origin = 'http://localhost:5173' }

$versionRouteId = "b0000000-0000-4000-8000-000000000001"
Write-Host "Consultando detalle de la ruta $versionRouteId..." -ForegroundColor Cyan

$routeDetail = Invoke-RestMethod -Uri "http://127.0.0.1:5173/api/v1/student/learning-routes/$versionRouteId" `
    -WebSession $session `
    -Headers @{ Origin = 'http://localhost:5173' }

$firstNode = $routeDetail.nodes[0]
Write-Host "Entrando al nodo #$($firstNode.sequence): $($firstNode.title) (Id: $($firstNode.id))" -ForegroundColor Yellow

# Iniciar actividad (entrar al nivel)
$activity = Invoke-RestMethod -Uri "http://127.0.0.1:5173/api/v1/student/learning-routes/$versionRouteId/nodes/$($firstNode.id)/start" `
    -Method Post `
    -WebSession $session `
    -Headers @{ Origin = 'http://localhost:5173' }

Write-Host "[EXITO] Actividad abierta correctamente:" -ForegroundColor Green
$activity | ConvertTo-Json -Depth 5
