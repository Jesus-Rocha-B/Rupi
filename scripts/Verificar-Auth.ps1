# Integration checks for student authentication against local Spring Boot and MySQL
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
. "$root\.local\env.ps1"
$mysql = "$root\.local\mysql-8.4.11-winx64\bin\mysql.exe"

function Sql([string]$query) {
    $query | & $mysql "--defaults-file=$root/.local/root.cnf" --database=rupi --batch
    if ($LASTEXITCODE -ne 0) { throw 'SQL verification failed' }
}

function Request([string]$path, [int]$expected, [string]$method = 'GET', [string]$body = $null, [hashtable]$headers = @{}) {
    $url = "http://127.0.0.1:8081/api/v1/$path"
    $actualStatus = 0
    $response = $null
    try {
        $params = @{
            Uri = $url
            Method = $method
            Headers = $headers
            UseBasicParsing = $true
        }
        if ($body) {
            $params['Body'] = $body
            $params['ContentType'] = 'application/json'
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

$originHeaders = @{ Origin = 'http://localhost:5173' }

try {
    Write-Host "--- 1. Validación de campos vacíos ---"
    $null = Request 'auth/login' 400 'POST' '{"username":"","password":""}' $originHeaders

    Write-Host "--- 2. Protección de Origen / CSRF ---"
    $null = Request 'auth/login' 403 'POST' '{"username":"estudiante.demo","password":"wrong"}' @{ Origin = 'https://malicious.example' }

    Write-Host "--- 3. Usuario inexistente (No enumeración -> 401) ---"
    $null = Request 'auth/login' 401 'POST' '{"username":"no_existe_usuario_xyz","password":"Password123!"}' $originHeaders

    Write-Host "--- 4. Contraseña incorrecta (401) ---"
    $null = Request 'auth/login' 401 'POST' '{"username":"estudiante.demo","password":"clave_incorrecta"}' $originHeaders

    Write-Host "--- 5. Ingreso exitoso con cuenta semilla dev ---"
    $loginResp = Request 'auth/login' 200 'POST' '{"username":"estudiante.demo","password":"123456"}' $originHeaders
    $studentData = $loginResp.Content | ConvertFrom-Json
    if ($studentData.name -ne 'Mateo') { throw "Expected student name Mateo, got $($studentData.name)" }
    if ($studentData.correo -or $studentData.apellido) { throw "Child privacy violated: leaked PII in student response" }

    $setCookie = $loginResp.Headers['Set-Cookie']
    if (-not $setCookie -or -not ($setCookie -match 'rupi_session=([^;]+)')) {
        throw "Cookie rupi_session not found in Set-Cookie header"
    }
    $sessionToken = $Matches[1]
    if (-not ($setCookie -match 'HttpOnly') -or -not ($setCookie -match 'SameSite=Strict')) {
        throw "Cookie security flags missing (HttpOnly, SameSite=Strict)"
    }

    Write-Host "--- 6. Acceso autenticado usando cookie rupi_session ---"
    $cookieHeaders = @{
        Cookie = "rupi_session=$sessionToken"
    }
    $sessionInfo = Request 'student/session' 200 'GET' $null $cookieHeaders
    $sessUser = $sessionInfo.Content | ConvertFrom-Json
    if ($sessUser.name -ne 'Mateo') { throw "Session resolved to unexpected user" }

    $routesResp = Request 'student/learning-routes' 200 'GET' $null $cookieHeaders
    $routesData = $routesResp.Content | ConvertFrom-Json
    if ($routesData.items.Count -eq 0) { throw "Expected enrolled routes for student" }

    Write-Host "--- 7. Cierre de sesión y revocación en base de datos ---"
    $logoutHeaders = @{
        Origin = 'http://localhost:5173'
        Cookie = "rupi_session=$sessionToken"
    }
    $logoutResp = Request 'auth/logout' 204 'POST' $null $logoutHeaders
    $clearCookie = $logoutResp.Headers['Set-Cookie']
    if (-not $clearCookie -or -not ($clearCookie -match 'Max-Age=0|max-age=0')) {
        throw "Logout did not clear rupi_session cookie"
    }

    Write-Host "--- 8. Verificación de expiración / revocación de sesión ---"
    $null = Request 'student/session' 401 'GET' $null $cookieHeaders
    $null = Request 'student/learning-routes' 401 'GET' $null $cookieHeaders

    Write-Host "--- 9. Verificación de mitigación de fuerza bruta (Rate Limiting) ---"
    $rateLimitUser = "ratelimit_test_$(Get-Random)"
    for ($i = 1; $i -le 5; $i++) {
        $null = Request 'auth/login' 401 'POST' "{`"username`":`"$rateLimitUser`",`"password`":`"wrong`"}" $originHeaders
    }
    # El 6to intento consecutivo debe recibir 429 Too Many Requests
    $null = Request 'auth/login' 429 'POST' "{`"username`":`"$rateLimitUser`",`"password`":`"wrong`"}" $originHeaders

    Write-Host "ALL RUPI STUDENT AUTHENTICATION INTEGRATION CHECKS PASSED!"
} finally {
    Sql "DELETE FROM identidad_intento_acceso WHERE ocurrido_en > UTC_TIMESTAMP(6) - INTERVAL 10 MINUTE;"
}
