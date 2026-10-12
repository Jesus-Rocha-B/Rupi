# Integration check for HU-11: Unirse a un Roadmap con Código de Clase Docente
# Compatible con Windows PowerShell 5.1 y PowerShell 7+
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
if (Test-Path "$root\.local\env.ps1") {
    . "$root\.local\env.ps1"
}

function Request([string]$path, [int]$expected, [string]$method = 'GET', [hashtable]$headers = @{}, [string]$body = $null) {
    $url = "http://127.0.0.1:8081/api/v1/$path"
    $response = $null
    $actualStatus = 0
    try {
        if (-not $headers.ContainsKey("Origin")) {
            $headers["Origin"] = "http://127.0.0.1:8081"
        }
        $params = @{
            Uri = $url
            Method = $method
            Headers = $headers
            UseBasicParsing = $true
        }
        if ($body) {
            $params["Body"] = $body
            if (-not $headers.ContainsKey("Content-Type")) {
                $headers["Content-Type"] = "application/json; charset=utf-8"
            }
        }
        $response = Invoke-WebRequest @params
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
    if ($actualStatus -ne $expected) {
        throw "Expected $expected for $method $path, got $actualStatus. Response: $($response.Content)"
    }
    Write-Host "PASS $expected $method $path"
    return $response
}

Write-Host "== Iniciando verificación HU-11: Unirse a Roadmap con Código de Clase =="
try {
    # 1. Protección de seguridad: acceso anónimo denegado (401 Unauthorized)
    Write-Host "`n1. Verificando protección del endpoint join-by-code..."
    Request "student/learning-routes/join-by-code" 401 "POST" @{"Content-Type"="application/json"} '{"code":"AYACUCHO2026"}' | Out-Null
    Write-Host "-> Correcto: Acceso anónimo denegado con 401."

    # 2. Iniciar sesión con estudiante demo
    Write-Host "`n2. Autenticando estudiante demo..."
    $loginBody = '{"username":"estudiante.demo","password":"123456"}'
    $loginResp = Invoke-WebRequest -Uri "http://127.0.0.1:8081/api/v1/auth/login" -Method POST `
        -Headers @{"Content-Type"="application/json"; "Origin"="http://127.0.0.1:8081"} -Body $loginBody -UseBasicParsing

    $cookieHeader = $loginResp.Headers["Set-Cookie"]
    $sessionCookie = ""
    if ($cookieHeader -match "rupi_session=([^;]+)") {
        $sessionCookie = $matches[1]
    } else {
        throw "No se encontró cookie rupi_session en la respuesta de login."
    }
    $authHeaders = @{
        "Cookie" = "rupi_session=$sessionCookie"
        "Content-Type" = "application/json; charset=utf-8"
    }
    Write-Host "-> Estudiante demo autenticado exitosamente."

    # 3. Validación de código en blanco o nulo (400 Bad Request)
    Write-Host "`n3. Validando rechazo de código en blanco o inválido (400 Bad Request)..."
    $respBlank = Request "student/learning-routes/join-by-code" 400 "POST" $authHeaders '{"code":""}'
    $jsonBlank = $respBlank.Content | ConvertFrom-Json
    if (-not ($jsonBlank.message -or $jsonBlank.detail -or $jsonBlank.title)) {
        throw "Se esperaba mensaje de error para código en blanco."
    }
    Write-Host "-> Correcto: Código en blanco rechazado con 400 y mensaje formativo."

    # 4. Validación de caracteres no permitidos o longitud mínima (400 Bad Request)
    Write-Host "`n4. Validando rechazo de código con caracteres no permitidos (400 Bad Request)..."
    $respChars = Request "student/learning-routes/join-by-code" 400 "POST" $authHeaders '{"code":"NO!VALID@#"}'
    $jsonChars = $respChars.Content | ConvertFrom-Json
    Write-Host "-> Correcto: Código con caracteres prohibidos rechazado con 400."

    # 5. Código no existente en la base de datos (404 Not Found)
    Write-Host "`n5. Probando código inexistente (404 Not Found)..."
    $respNotFound = Request "student/learning-routes/join-by-code" 404 "POST" $authHeaders '{"code":"INVENTADO999"}'
    $jsonNotFound = $respNotFound.Content | ConvertFrom-Json
    $notFoundMsg = $jsonNotFound.message
    if (-not $notFoundMsg) { $notFoundMsg = $jsonNotFound.detail }
    if (-not ($notFoundMsg -match "no existe")) {
        throw "Se esperaba mensaje indicando que el código no existe, recibido: $notFoundMsg"
    }
    Write-Host "-> Correcto: Código inexistente retorna 404 con mensaje claro: '$notFoundMsg'."

    # 6. Código expirado / fuera de vigencia (400 Bad Request)
    Write-Host "`n6. Probando código vencido 'EXPIRADO2025' (400 Bad Request)..."
    $respExpired = Request "student/learning-routes/join-by-code" 400 "POST" $authHeaders '{"code":"EXPIRADO2025"}'
    $jsonExpired = $respExpired.Content | ConvertFrom-Json
    $expiredMsg = $jsonExpired.message
    if (-not $expiredMsg) { $expiredMsg = $jsonExpired.detail }
    if (-not ($expiredMsg -match "venció|expirado")) {
        throw "Se esperaba mensaje indicando vigencia vencida, recibido: $expiredMsg"
    }
    Write-Host "-> Correcto: Código expirado retorna 400 con mensaje: '$expiredMsg'."

    # 7. Código revocado por docente (400 Bad Request)
    Write-Host "`n7. Probando código revocado 'REVOCADO2026' (400 Bad Request)..."
    $respRevoked = Request "student/learning-routes/join-by-code" 400 "POST" $authHeaders '{"code":"REVOCADO2026"}'
    $jsonRevoked = $respRevoked.Content | ConvertFrom-Json
    $revokedMsg = $jsonRevoked.message
    if (-not $revokedMsg) { $revokedMsg = $jsonRevoked.detail }
    if (-not ($revokedMsg -match "revocado")) {
        throw "Se esperaba mensaje indicando que fue revocado, recibido: $revokedMsg"
    }
    Write-Host "-> Correcto: Código revocado retorna 400 con mensaje: '$revokedMsg'."

    # 8. Unirse exitosamente a una clase activa con código 'WARI2026' (3.° Matemática)
    Write-Host "`n8. Unirse a clase escolar activa con código 'WARI2026'..."
    $respJoin = Request "student/learning-routes/join-by-code" 200 "POST" $authHeaders '{"code":"WARI2026"}'
    $jsonJoin = $respJoin.Content | ConvertFrom-Json
    if (-not $jsonJoin.versionRouteId) {
        throw "Se esperaba versionRouteId en la respuesta."
    }
    if ($jsonJoin.grade.number -ne 3) {
        throw "Se esperaba grado 3 para el código WARI2026, recibido: $($jsonJoin.grade.number)"
    }
    Write-Host "-> Éxito: Estudiante vinculado a la ruta '$($jsonJoin.routeTitle)' (Grado: $($jsonJoin.grade.name), Área: $($jsonJoin.area.name))."
    Write-Host "-> Mensaje devuelto: '$($jsonJoin.message)' (alreadyEnrolled=$($jsonJoin.alreadyEnrolled))."

    # 9. Idempotencia: intentar unirse de nuevo con el mismo código
    Write-Host "`n9. Probando idempotencia al reingresar el mismo código 'WARI2026'..."
    $respRejoin = Request "student/learning-routes/join-by-code" 200 "POST" $authHeaders '{"code":"wari2026"}'
    $jsonRejoin = $respRejoin.Content | ConvertFrom-Json
    if (-not $jsonRejoin.alreadyEnrolled) {
        throw "Se esperaba alreadyEnrolled = true al reintentar unirse a una clase ya vinculada."
    }
    Write-Host "-> Correcto: Idempotencia validada con éxito. alreadyEnrolled = true. Mensaje: '$($jsonRejoin.message)'."

    # 10. Redirección y acceso al mapa interactivo de la ruta vinculada
    Write-Host "`n10. Verificando acceso inmediato a la ruta escolar vinculada ($($jsonJoin.versionRouteId))..."
    $respRoute = Request "student/learning-routes/$($jsonJoin.versionRouteId)" 200 "GET" $authHeaders
    $jsonRoute = $respRoute.Content | ConvertFrom-Json
    if ($jsonRoute.nodes.Count -eq 0) {
        throw "La ruta vinculada debe contener nodos de aprendizaje para el mapa interactivo."
    }
    Write-Host "-> Mapa interactivo accesible: $($jsonRoute.nodes.Count) paradas cargadas correctamente."

    Write-Host "`n============================================================"
    Write-Host "==   TODAS LAS VERIFICACIONES DE HU-11 PASARON CON ÉXITO   =="
    Write-Host "============================================================"
} catch {
    Write-Error $_
    exit 1
}
