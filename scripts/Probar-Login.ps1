$ErrorActionPreference = 'Stop'

Write-Host "Probando login de estudiante (estudiante.demo)..." -ForegroundColor Yellow
$bodyStudent = '{"username":"estudiante.demo","password":"123456"}'
$resStudent = Invoke-RestMethod -Uri "http://127.0.0.1:8081/api/v1/auth/login" `
    -Method Post `
    -ContentType "application/json" `
    -Body $bodyStudent `
    -Headers @{ Origin = "http://localhost:5173" }

Write-Host "[OK] Estudiante autenticado:" -ForegroundColor Green
$resStudent | ConvertTo-Json -Depth 5

Write-Host "`nProbando login de docente (profesor.demo)..." -ForegroundColor Yellow
$bodyTeacher = '{"username":"profesor.demo","password":"123456"}'
$resTeacher = Invoke-RestMethod -Uri "http://127.0.0.1:8081/api/v1/auth/login" `
    -Method Post `
    -ContentType "application/json" `
    -Body $bodyTeacher `
    -Headers @{ Origin = "http://localhost:5173" }

Write-Host "[OK] Docente autenticado:" -ForegroundColor Green
$resTeacher | ConvertTo-Json -Depth 5
