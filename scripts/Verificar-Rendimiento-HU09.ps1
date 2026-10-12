# Performance and Stress Test for HU-09: Búsqueda y Autocompletado de Roadmaps con FTS
# Compatible con Windows PowerShell 5.1 y PowerShell 7+
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot

Write-Host "=========================================================================="
Write-Host " RUPI · Pruebas de Rendimiento y Carga de Búsqueda con FTS (HU-09)"
Write-Host "=========================================================================="

# 1. Autenticar estudiante demo para obtener sesión
$loginBody = '{"username":"estudiante.demo","password":"123456"}'
$loginResp = Invoke-WebRequest -Uri "http://127.0.0.1:8081/api/v1/auth/login" -Method POST `
    -Headers @{"Content-Type"="application/json"; "Origin"="http://127.0.0.1:8081"} -Body $loginBody -UseBasicParsing

$cookieHeader = $loginResp.Headers["Set-Cookie"] -join "; "
$sessionCookie = ""
if ($cookieHeader -match "rupi_session=([^;]+)") {
    $sessionCookie = $matches[1]
}
if (-not $sessionCookie) {
    throw "No se pudo obtener la cookie de sesión."
}
$authHeaders = @{"Cookie"="rupi_session=$sessionCookie"}

# 2. Batería de consultas representativas de estudiantes de primaria
$testQueries = @(
    "matematica",
    "comunicacion",
    "ciencia",
    "arte",
    "social",
    "retablo",
    "biodiversidad",
    "geometria",
    "andina",
    "com",
    "mat",
    "cie"
)

Write-Host "`nEjecutando pruebas de latencia y volumen sobre consultas FTS..."
$latencies = [System.Collections.Generic.List[double]]::new()
$totalIterations = 60

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

for ($i = 1; $i -le $totalIterations; $i++) {
    $q = $testQueries[$i % $testQueries.Length]
    $queryStopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    
    $url = "http://127.0.0.1:8081/api/v1/student/learning-routes/search?q=$q&limit=20"
    $resp = Invoke-WebRequest -Uri $url -Headers $authHeaders -UseBasicParsing
    
    $queryStopwatch.Stop()
    $elapsedMs = $queryStopwatch.Elapsed.TotalMilliseconds
    $latencies.Add($elapsedMs)
    
    if ($resp.StatusCode -ne 200) {
        throw "Consulta fallida con status $($resp.StatusCode)"
    }
}

$stopwatch.Stop()
$totalTimeMs = $stopwatch.Elapsed.TotalMilliseconds

# 3. Métricas estadísticas de rendimiento
$sortedLatencies = $latencies | Sort-Object
$avgLatency = ($latencies | Measure-Object -Average).Average
$minLatency = ($latencies | Measure-Object -Minimum).Minimum
$maxLatency = ($latencies | Measure-Object -Maximum).Maximum
$p50Index = [Math]::Floor($sortedLatencies.Count * 0.50)
$p90Index = [Math]::Floor($sortedLatencies.Count * 0.90)
$p99Index = [Math]::Min($sortedLatencies.Count - 1, [Math]::Floor($sortedLatencies.Count * 0.99))
$p50 = $sortedLatencies[$p50Index]
$p90 = $sortedLatencies[$p90Index]
$p99 = $sortedLatencies[$p99Index]
$throughput = [Math]::Round(($totalIterations / ($totalTimeMs / 1000.0)), 2)

Write-Host "`n--------------------------------------------------------------------------"
Write-Host " RESULTADOS DE RENDIMIENTO Y LATENCIA (HU-09):"
Write-Host "--------------------------------------------------------------------------"
Write-Host " Total de peticiones ejecutadas : $totalIterations"
Write-Host " Tiempo total acumulado         : $([Math]::Round($totalTimeMs, 2)) ms"
Write-Host " Throughput estimado            : $throughput req/s"
Write-Host " Latencia Mínima                : $([Math]::Round($minLatency, 2)) ms"
Write-Host " Latencia Promedio              : $([Math]::Round($avgLatency, 2)) ms"
Write-Host " Latencia P50 (Mediana)         : $([Math]::Round($p50, 2)) ms"
Write-Host " Latencia P90                   : $([Math]::Round($p90, 2)) ms"
Write-Host " Latencia P99 (Peor caso)       : $([Math]::Round($p99, 2)) ms"
Write-Host " Latencia Máxima                : $([Math]::Round($maxLatency, 2)) ms"
Write-Host "--------------------------------------------------------------------------"

# Validación de umbral de aceptación: P90 < 80ms y Promedio < 50ms
if ($avgLatency -lt 50.0 -and $p90 -lt 80.0) {
    Write-Host "`n-> RESULTADO: [APROBADO] Rendimiento ultra-rápido (< 50ms promedio)." -ForegroundColor Green
    Write-Host "   El índice Full-Text Search y el filtrado en base de datos responden instantáneamente."
} else {
    Write-Host "`n-> RESULTADO: [ADVERTENCIA] Latencia promedio ($avgLatency ms) excedió el objetivo recomendado de 50ms." -ForegroundColor Yellow
}
