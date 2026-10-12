# Integration check for HU-09: Búsqueda y Autocompletado de Roadmaps Alternativos por Materia
# Compatible con Windows PowerShell 5.1 y PowerShell 7+
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
if (Test-Path "$root\.local\env.ps1") {
    . "$root\.local\env.ps1"
}

function Request([string]$path, [int]$expected, [string]$method = 'GET', [hashtable]$headers = @{}) {
    $url = "http://127.0.0.1:8081/api/v1/$path"
    $response = $null
    $actualStatus = 0
    try {
        $response = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -UseBasicParsing
        $actualStatus = [int]$response.StatusCode
    } catch [System.Net.WebException] {
        if ($_.Exception.Response) {
            $webResp = [System.Net.HttpWebResponse]$_.Exception.Response
            $actualStatus = [int]$webResp.StatusCode
            $content = $null
            $stream = $webResp.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $content = $reader.ReadToEnd()
            }
            $respHeaders = @{}
            foreach ($k in $webResp.Headers.AllKeys) {
                $respHeaders[$k] = $webResp.Headers[$k]
            }
            $response = [PSCustomObject]@{
                StatusCode = $actualStatus
                Content = $content
                Headers = $respHeaders
            }
        } else {
            throw $_
        }
    }
    if ($actualStatus -ne $expected) { throw "Expected $expected for $method $path, got $actualStatus" }
    Write-Host "PASS $expected $method $path"
    return $response
}

Write-Host "== Iniciando verificación HU-09: Búsqueda de Roadmaps por Materia y Autocompletado =="
try {
    # 1. Verificar protección sin sesión (401 Unauthorized)
    Write-Host "`n1. Verificando protección de endpoints de búsqueda..."
    Request "student/learning-routes/search?q=matematica" 401 | Out-Null
    Request "student/learning-routes/suggestions?q=mat" 401 | Out-Null
    Write-Host "-> Correcto: Acceso anónimo denegado con 401."

    # 2. Iniciar sesión con estudiante demo
    Write-Host "`n2. Autenticando estudiante demo..."
    $loginBody = '{"username":"estudiante.demo","password":"123456"}'
    $loginResp = Invoke-WebRequest -Uri "http://127.0.0.1:8081/api/v1/auth/login" -Method POST `
        -Headers @{"Content-Type"="application/json"} -Body $loginBody -UseBasicParsing

    $cookieHeader = $loginResp.Headers["Set-Cookie"]
    $sessionCookie = ""
    if ($cookieHeader -match "rupi_session=([^;]+)") {
        $sessionCookie = $matches[1]
    }
    if (-not $sessionCookie) {
        throw "No se pudo obtener la cookie rupi_session."
    }
    $authHeaders = @{"Cookie"="rupi_session=$sessionCookie"}
    Write-Host "-> Sesión iniciada correctamente."

    # 3. Probar sugerencias de autocompletado en tiempo real
    Write-Host "`n3. Probando endpoint de sugerencias de autocompletado (/suggestions)..."
    $suggResp = Request "student/learning-routes/suggestions?q=com" 200 "GET" $authHeaders
    $suggJson = $suggResp.Content | ConvertFrom-Json
    Write-Host "-> Sugerencias recibidas para 'com': $($suggJson -join ', ')"
    if ($suggJson.Count -eq 0) {
        throw "Se esperaba al menos una sugerencia para 'com'."
    }

    # 4. Probar búsqueda de roadmaps por nombre de materia (Comunicación)
    Write-Host "`n4. Probando búsqueda de roadmaps por materia (/search?q=comunicación)..."
    $searchResp = Request "student/learning-routes/search?q=comunicaci%C3%B3n" 200 "GET" $authHeaders
    $searchJson = $searchResp.Content | ConvertFrom-Json
    Write-Host "-> Roadmaps encontrados: $($searchJson.routes.Count)"
    foreach ($r in $searchJson.routes) {
        Write-Host "   - [$($r.areaName)] $($r.routeTitle) (Grado: $($r.gradeName), Paradas: $($r.totalNodes), Inscrito: $($r.isEnrolled))"
    }
    if ($searchJson.routes.Count -eq 0) {
        throw "Se esperaban roadmaps de Comunicación en los resultados de búsqueda."
    }

    # 5. Probar búsqueda con filtro de grado
    Write-Host "`n5. Probando búsqueda filtrada por grado (/search?q=matematica&grade=2)..."
    $gradeSearchResp = Request "student/learning-routes/search?q=matematica&grade=2" 200 "GET" $authHeaders
    $gradeSearchJson = $gradeSearchResp.Content | ConvertFrom-Json
    Write-Host "-> Roadmaps de Matemática para 2.° de primaria: $($gradeSearchJson.routes.Count)"
    if ($gradeSearchJson.routes.Count -eq 0) {
        throw "Se esperaba encontrar la ruta de Matemática para 2.° de primaria."
    }
    $enrolledRoute = $gradeSearchJson.routes | Where-Object { $_.isEnrolled -eq $true }
    if (-not $enrolledRoute) {
        throw "La ruta inscrita del estudiante demo debería marcar isEnrolled = true."
    }
    Write-Host "-> Correcto: Ruta activa identificada ($($enrolledRoute.routeTitle))."

    # 6. Probar búsqueda con término sin coincidencias
    Write-Host "`n6. Probando búsqueda sin coincidencias (/search?q=astronomia)..."
    $emptySearchResp = Request "student/learning-routes/search?q=astronomia" 200 "GET" $authHeaders
    $emptySearchJson = $emptySearchResp.Content | ConvertFrom-Json
    if ($emptySearchJson.routes.Count -ne 0) {
        throw "No se esperaban resultados para un término inexistente."
    }
    Write-Host "-> Correcto: 0 resultados devueltos de forma segura."

    Write-Host "`n================================================================="
    Write-Host "¡TODAS LAS PRUEBAS DE INTEGRACIÓN DE HU-09 PASARON EXITOSAMENTE!"
    Write-Host "================================================================="
} catch {
    Write-Error "Fallo en verificación HU-09: $_"
    exit 1
}
