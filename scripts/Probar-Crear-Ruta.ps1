$ErrorActionPreference = 'Stop'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-RestMethod -Uri 'http://127.0.0.1:5173/api/v1/auth/login' `
    -Method Post `
    -ContentType 'application/json' `
    -Body '{"username":"profesor.demo","password":"123456"}' `
    -WebSession $session `
    -Headers @{ Origin = 'http://localhost:5173' } | Out-Null

Write-Host "Sesion docente iniciada." -ForegroundColor Yellow

$routeBody = '{"title":"Ruta de Prueba Docente","description":"Probando creacion","gradeNumber":3,"areaCode":"COM"}'
$res = Invoke-RestMethod -Uri 'http://127.0.0.1:5173/api/v1/teacher/learning-routes' `
    -Method Post `
    -ContentType 'application/json' `
    -Body $routeBody `
    -WebSession $session `
    -Headers @{ Origin = 'http://localhost:5173' }

Write-Host "Ruta creada exitosamente:" -ForegroundColor Green
$res | ConvertTo-Json -Depth 5

$routeId = $res.routeId

# Probar agregar parada
$nodeBody = '{"title":"Parada 1: Las Leyendas Andinas","instructions":"Lee con atencion el relato tradicional.","activityType":"LECCION","estimatedMinutes":15}'
$resNode = Invoke-RestMethod -Uri "http://127.0.0.1:5173/api/v1/teacher/learning-routes/$routeId/nodes" `
    -Method Post `
    -ContentType 'application/json' `
    -Body $nodeBody `
    -WebSession $session `
    -Headers @{ Origin = 'http://localhost:5173' }

Write-Host "Parada agregada exitosamente:" -ForegroundColor Green
$resNode | ConvertTo-Json -Depth 5

$routeDetail = Invoke-RestMethod -Uri "http://127.0.0.1:5173/api/v1/teacher/learning-routes/$routeId" `
    -WebSession $session `
    -Headers @{ Origin = 'http://localhost:5173' }

Write-Host "Detalle del roadmap obtenido exitosamente (total nodos: $($routeDetail.nodes.Count)):" -ForegroundColor Green
$routeDetail | ConvertTo-Json -Depth 5
