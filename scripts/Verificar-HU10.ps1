# Integration check for HU-10: Filtros Avanzados por Grado Escolar y Materia
# Compatible con Windows PowerShell 5.1 y PowerShell 7+
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$envFile = Join-Path (Join-Path $root '.local') 'env.ps1'
if (Test-Path $envFile) {
    . $envFile
}

function Request([string]$path, [int]$expected, [string]$method = 'GET', [hashtable]$headers = @{}) {
    $url = "http://127.0.0.1:8081/api/v1/$path"
    $response = $null
    $actualStatus = 0
    try {
        $response = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -UseBasicParsing
        $actualStatus = [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            $webResp = $_.Exception.Response
            $actualStatus = [int]$webResp.StatusCode
            $content = $null
            $respHeaders = @{}
            if ($PSVersionTable.PSVersion.Major -ge 6) {
                $content = $_.ErrorDetails.Message
                foreach ($h in $webResp.Headers) { $respHeaders[$h.Key] = ($h.Value -join ', ') }
            } else {
                $stream = $webResp.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $content = $reader.ReadToEnd()
                }
                foreach ($k in $webResp.Headers.AllKeys) { $respHeaders[$k] = $webResp.Headers[$k] }
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

Write-Host "== Iniciando verificación HU-10: Filtros Avanzados por Grado y Materia =="
try {
    # 1. Verificar protección sin sesión (401 Unauthorized)
    Write-Host "`n1. Verificando protección de endpoints de búsqueda y filtros..."
    Request "student/learning-routes/search?grades=3,4&areas=MATEMATICA" 401 | Out-Null
    Write-Host "-> Correcto: Acceso anónimo denegado con 401."

    # 2. Iniciar sesión con estudiante demo
    Write-Host "`n2. Autenticando estudiante demo..."
    $loginBody = '{"username":"estudiante.demo","password":"123456"}'
    $loginResp = Invoke-WebRequest -Uri "http://127.0.0.1:8081/api/v1/auth/login" -Method POST `
        -Headers @{"Content-Type"="application/json"; "Origin"="http://127.0.0.1:8081"} -Body $loginBody -UseBasicParsing

    $cookieHeader = $loginResp.Headers["Set-Cookie"] -join "; "
    $sessionCookie = ""
    if ($cookieHeader -match "rupi_session=([^;]+)") {
        $sessionCookie = $matches[1]
    } else {
        throw "No se encontró cookie rupi_session en la respuesta de login."
    }
    $authHeaders = @{
        "Cookie" = "rupi_session=$sessionCookie"
    }
    Write-Host "-> Estudiante demo autenticado exitosamente."

    # 3. Filtrar por un grado escolar específico (3.° de primaria)
    Write-Host "`n3. Probando filtro por grado único (3.° de primaria)..."
    $respGrade3 = Request "student/learning-routes/search?grades=3" 200 "GET" $authHeaders
    $jsonGrade3 = $respGrade3.Content | ConvertFrom-Json
    if ($jsonGrade3.routes.Count -eq 0) {
        throw "Se esperaba al menos una ruta para 3.° de primaria."
    }
    foreach ($r in $jsonGrade3.routes) {
        if ($r.gradeNumber -ne 3) {
            throw "Precisión fallida: ruta $($r.routeTitle) tiene grado $($r.gradeNumber) y se esperaba 3."
        }
    }
    Write-Host "-> Correcto: $($jsonGrade3.routes.Count) rutas devueltas pertenecen exclusivamente a 3.° de primaria."

    # 4. Filtrar por múltiples grados simultáneos (3.° y 4.° de primaria)
    Write-Host "`n4. Probando filtro por múltiples grados simultáneos (3.° y 4.° de primaria)..."
    $respMultiGrades = Request "student/learning-routes/search?grades=3,4" 200 "GET" $authHeaders
    $jsonMultiGrades = $respMultiGrades.Content | ConvertFrom-Json
    if ($jsonMultiGrades.routes.Count -eq 0) {
        throw "Se esperaban rutas para 3.° y 4.° de primaria."
    }
    foreach ($r in $jsonMultiGrades.routes) {
        if ($r.gradeNumber -ne 3 -and $r.gradeNumber -ne 4) {
            throw "Precisión fallida: ruta $($r.routeTitle) tiene grado $($r.gradeNumber) y se esperaba 3 o 4."
        }
    }
    Write-Host "-> Correcto: $($jsonMultiGrades.routes.Count) rutas devueltas pertenecen exclusivamente a los grados 3 o 4."

    # 5. Filtrar por materia / área curricular (Matemática)
    Write-Host "`n5. Probando filtro por materia (Matemática)..."
    $respMat = Request "student/learning-routes/search?areas=MATEMATICA" 200 "GET" $authHeaders
    $jsonMat = $respMat.Content | ConvertFrom-Json
    if ($jsonMat.routes.Count -eq 0) {
        throw "Se esperaban rutas de Matemática."
    }
    foreach ($r in $jsonMat.routes) {
        if ($r.areaCode -ne "MAT" -and $r.areaName -ne "Matemática") {
            throw "Precisión fallida: ruta $($r.routeTitle) tiene área $($r.areaCode) y se esperaba MAT."
        }
    }
    Write-Host "-> Correcto: $($jsonMat.routes.Count) rutas devueltas pertenecen exclusivamente a Matemática."

    # 6. Combinación simultánea: Múltiples grados Y Materia (3.° y 4.° en Matemática)
    Write-Host "`n6. Probando combinación de filtros: Grados (3 y 4) + Materia (Matemática)..."
    $respCombo = Request "student/learning-routes/search?grades=3,4&areas=MATEMATICA" 200 "GET" $authHeaders
    $jsonCombo = $respCombo.Content | ConvertFrom-Json
    if ($jsonCombo.routes.Count -eq 0) {
        throw "Se esperaban rutas para la combinación 3.°/4.° y Matemática."
    }
    foreach ($r in $jsonCombo.routes) {
        if (($r.gradeNumber -ne 3 -and $r.gradeNumber -ne 4) -or ($r.areaCode -ne "MAT")) {
            throw "Precisión fallida en filtro combinado: grado=$($r.gradeNumber), área=$($r.areaCode)."
        }
    }
    Write-Host "-> Correcto: $($jsonCombo.routes.Count) rutas encontradas coinciden exactamente con grado (3 o 4) Y materia (MAT)."

    # 7. Combinación con búsqueda de texto: Grado 1 + Comunicación + término 'rimas' o 'vocales'
    Write-Host "`n7. Probando combinación Grado 1 + Comunicación + búsqueda de texto..."
    $respText = Request "student/learning-routes/search?grades=1&areas=COM&q=vocales" 200 "GET" $authHeaders
    $jsonText = $respText.Content | ConvertFrom-Json
    if ($jsonText.routes.Count -eq 0) {
        throw "Se esperaba la ruta de Comunicación de 1.° grado para la búsqueda 'vocales'."
    }
    Write-Host "-> Correcto: Se encontró la ruta '$($jsonText.routes[0].routeTitle)' de 1.° de primaria."

    # 8. Filtro sin coincidencias
    Write-Host "`n8. Probando filtros sin resultados..."
    $respEmpty = Request "student/learning-routes/search?grades=1&areas=ART&q=invalido123" 200 "GET" $authHeaders
    $jsonEmpty = $respEmpty.Content | ConvertFrom-Json
    if ($jsonEmpty.routes.Count -ne 0) {
        throw "Se esperaba 0 resultados para búsqueda sin coincidencias."
    }
    Write-Host "-> Correcto: Lista vacía devuelta limpiamente (totalResults: $($jsonEmpty.totalResults))."

    Write-Host "`n========================================================"
    Write-Host "   TODAS LAS PRUEBAS DE HU-10 PASARON EXITOSAMENTE"
    Write-Host "========================================================"
} catch {
    Write-Error "Error en verificación HU-10: $_"
    exit 1
}
