# ==============================================================================
# Verificacion - HU-12: Descargar materiales y ejercicios de una unidad (sin conexion)
# ==============================================================================
# Compatible con Windows PowerShell 5.1 y PowerShell 7+.
# Requiere la API corriendo (por defecto http://127.0.0.1:8081) y la cuenta demo.
#
#   ./scripts/Verificar-HU12-Descarga.ps1
#   ./scripts/Verificar-HU12-Descarga.ps1 -ApiBase http://127.0.0.1:8081 -RutaId <versionRouteId>

param(
    [string]$ApiBase = 'http://127.0.0.1:8081',
    [string]$Usuario = 'estudiante.demo',
    [string]$Clave = '123456',
    [string]$Origen = 'http://127.0.0.1:5173',
    [string]$RutaId = ''
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$script:pass = 0
$script:fail = 0
function Pass([string]$name) { $script:pass++; Write-Host " [PASS] $name" -ForegroundColor Green }
function Fail([string]$name, [string]$reason) { $script:fail++; Write-Host " [FAIL] $name - $reason" -ForegroundColor Red }

function Get-Header($response, [string]$name) {
    $value = $response.Headers[$name]
    if ($null -eq $value) { return '' }
    return [string]($value | Select-Object -First 1)
}

function Get-StatusCode([scriptblock]$call) {
    try {
        $r = & $call
        return [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        throw
    }
}

function Get-Sha256Hex([byte[]]$bytes) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { return ([System.BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
}

function Read-ZipEntries([string]$path) {
    $entries = @{}
    $zip = [System.IO.Compression.ZipFile]::OpenRead($path)
    try {
        foreach ($entry in $zip.Entries) {
            $stream = $entry.Open()
            $memory = New-Object System.IO.MemoryStream
            try { $stream.CopyTo($memory); $entries[$entry.FullName] = $memory.ToArray() }
            finally { $stream.Dispose(); $memory.Dispose() }
        }
    } finally { $zip.Dispose() }
    return $entries
}

$api = "$ApiBase/api/v1"
$work = Join-Path ([System.IO.Path]::GetTempPath()) ("rupi-hu12-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " VERIFICACION HU-12: PAQUETE DE UNIDAD PARA ESTUDIAR SIN CONEXION" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

try {
    Write-Host "`n1. Sesion del estudiante" -ForegroundColor Yellow
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $body = @{ username = $Usuario; password = $Clave } | ConvertTo-Json
    $login = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$api/auth/login" -WebSession $session `
        -ContentType 'application/json' -Headers @{ Origin = $Origen } -Body $body
    if ([int]$login.StatusCode -eq 200) { Pass "Login de $Usuario" } else { Fail "Login" "HTTP $($login.StatusCode)" }

    if (-not $RutaId) {
        $routes = Invoke-RestMethod -Uri "$api/student/learning-routes" -WebSession $session
        foreach ($item in $routes.items) {
            $candidate = Invoke-RestMethod -Uri "$api/student/learning-routes/$($item.versionRouteId)" -WebSession $session
            if ($candidate.curriculum -and @($candidate.curriculum.units).Count -gt 0) { $RutaId = $item.versionRouteId; break }
        }
    }
    if (-not $RutaId) { throw 'El estudiante no tiene ninguna ruta agrupada por unidades.' }
    $route = Invoke-RestMethod -Uri "$api/student/learning-routes/$RutaId" -WebSession $session
    $units = @($route.curriculum.units)
    Pass "Ruta '$($route.title)' con $($units.Count) unidad(es)"

    Write-Host "`n2. Descarga, integridad y lectura de cada unidad" -ForegroundColor Yellow
    foreach ($unit in $units) {
        $key = if ($unit.id) { $unit.id } else { 'otras' }
        $url = "$api/student/learning-routes/$RutaId/units/$key/package"
        $file = Join-Path $work "$key.zip"
        $label = "Unidad '$($unit.title)'"

        $first = Invoke-WebRequest -UseBasicParsing -Uri $url -WebSession $session -OutFile $file -PassThru
        $bytes = [System.IO.File]::ReadAllBytes($file)
        $contentType = Get-Header $first 'Content-Type'
        $announced = Get-Header $first 'X-Rupi-Paquete-Sha256'

        if ([int]$first.StatusCode -eq 200 -and $contentType -like 'application/zip*') { Pass "$label se descarga como zip" }
        else { Fail "$label descarga" "HTTP $($first.StatusCode), tipo '$contentType'"; continue }

        if ($announced -and $announced -eq (Get-Sha256Hex $bytes)) { Pass "$label llega completa (SHA-256 coincide con la cabecera)" }
        else { Fail "$label integridad" "SHA-256 recibido no coincide con '$announced'" }

        $entries = Read-ZipEntries $file
        foreach ($required in @('guia-de-estudio.pdf', 'contenido.json', 'manifest.json', 'LEEME.txt')) {
            if (-not $entries.ContainsKey($required)) { Fail "$label contenido" "falta $required" }
        }

        $manifest = [System.Text.Encoding]::UTF8.GetString($entries['manifest.json']) | ConvertFrom-Json
        $broken = @()
        foreach ($item in $manifest.archivos) {
            $data = $entries[$item.ruta]
            if ($null -eq $data -or $data.Length -ne [int64]$item.bytes -or (Get-Sha256Hex $data) -ne $item.sha256) { $broken += $item.ruta }
        }
        if ($broken.Count -eq 0) { Pass "$label manifest.json: $(@($manifest.archivos).Count) archivos con tamano y SHA-256 correctos" }
        else { Fail "$label manifest.json" ("no cuadran: " + ($broken -join ', ')) }
        if (@($manifest.materialesNoIncluidos).Count -gt 0) {
            Write-Host "        Aviso: materiales no incluidos -> $($manifest.materialesNoIncluidos -join '; ')" -ForegroundColor DarkYellow
        }

        $pdf = $entries['guia-de-estudio.pdf']
        $pdfHead = [System.Text.Encoding]::ASCII.GetString($pdf, 0, 5)
        $pdfText = [System.Text.Encoding]::GetEncoding('ISO-8859-1').GetString($pdf)
        $remote = [regex]::Matches($pdfText, '/(URI|Launch|GoToR|JavaScript)\b').Count
        if ($pdfHead -eq '%PDF-' -and $pdfText.TrimEnd().EndsWith('%%EOF') -and $remote -eq 0) {
            Pass "$label guia PDF valida y sin recursos remotos ($([math]::Round($pdf.Length / 1KB, 1)) KB)"
        } else { Fail "$label guia PDF" "cabecera '$pdfHead', acciones remotas: $remote" }

        $second = Invoke-WebRequest -UseBasicParsing -Uri $url -WebSession $session
        if ((Get-Header $second 'X-Rupi-Paquete-Cache') -eq 'HIT' -and (Get-Header $second 'X-Rupi-Paquete-Sha256') -eq $announced) {
            Pass "$label segunda descarga sale de la cache"
        } else { Fail "$label cache" "esperado HIT con el mismo SHA-256" }
    }

    Write-Host "`n3. Accesos no permitidos" -ForegroundColor Yellow
    $anyUnit = if ($units[0].id) { $units[0].id } else { 'otras' }
    $anon = Get-StatusCode { Invoke-WebRequest -UseBasicParsing -Uri "$api/student/learning-routes/$RutaId/units/$anyUnit/package" }
    if ($anon -eq 401) { Pass "Sin sesion responde 401" } else { Fail "Sin sesion" "HTTP $anon" }

    $unknown = Get-StatusCode { Invoke-WebRequest -UseBasicParsing -WebSession $session -Uri "$api/student/learning-routes/$RutaId/units/$([guid]::NewGuid())/package" }
    if ($unknown -eq 404) { Pass "Unidad ajena a la ruta responde 404" } else { Fail "Unidad ajena" "HTTP $unknown" }

    $otherRoute = Get-StatusCode { Invoke-WebRequest -UseBasicParsing -WebSession $session -Uri "$api/student/learning-routes/$([guid]::NewGuid())/units/$anyUnit/package" }
    if ($otherRoute -eq 404) { Pass "Ruta sin inscripcion responde 404" } else { Fail "Ruta sin inscripcion" "HTTP $otherRoute" }
} catch {
    Fail 'Ejecucion' $_.Exception.Message
} finally {
    Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue
}

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host " Resultado: $script:pass correctas, $script:fail con fallas" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
if ($script:fail -gt 0) { exit 1 }
exit 0
