<#
.SYNOPSIS
    Script de reinicio y recuperación automática de servicios críticos de RUPI (HU-47).
.DESCRIPTION
    Restablece inmediatamente los servicios caídos (MySQL, IA FastAPI o API Spring Boot).
    Compatible con Windows PowerShell 5.1 nativo y PowerShell 7+.
.PARAMETER Servicio
    Nombre del servicio a reiniciar: 'mysql', 'ia', 'api' o 'todos'.
.PARAMETER Forzar
    Fuerza el cierre inmediato de procesos zombies que bloqueen los puertos.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('mysql', 'ia', 'api', 'todos')]
    [string]$Servicio,

    [switch]$Forzar
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path "$PSScriptRoot\.."
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

# Cargar variables de entorno locales
$envFile = "$root\.local\env.ps1"
if (Test-Path $envFile) {
    . $envFile
}

function Test-TcpPort([int]$Port) {
    $client = New-Object Net.Sockets.TcpClient
    try {
        $ar = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        $success = $ar.AsyncWaitHandle.WaitOne(800, $false)
        if ($success -and $client.Connected) {
            $client.EndConnect($ar)
            return $true
        }
        return $false
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Wait-TcpPort([int]$Port, [int]$TimeoutSeconds = 30) {
    for ($i = 0; $i -lt $TimeoutSeconds; $i++) {
        if (Test-TcpPort $Port) { return $true }
        Start-Sleep -Milliseconds 1000
    }
    return $false
}

function Kill-ProcessOnPort([int]$Port) {
    try {
        $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        foreach ($c in $conns) {
            if ($c.OwningProcess -and $c.OwningProcess -gt 4) {
                Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
            }
        }
    } catch {
        # Si no se tiene permisos de NetTCPConnection, continuar
    }
}

function Stop-RupiProcess([string]$PidFileName, [int]$Port) {
    $pidPath = "$root\.local\$PidFileName"
    if (Test-Path $pidPath) {
        try {
            $savedId = [int](Get-Content $pidPath -ErrorAction SilentlyContinue)
            if ($savedId -gt 0) {
                $p = Get-Process -Id $savedId -ErrorAction SilentlyContinue
                if ($p) { Stop-Process -Id $savedId -Force -ErrorAction SilentlyContinue }
            }
        } catch {}
        Remove-Item -LiteralPath $pidPath -Force -ErrorAction SilentlyContinue
    }
    if ($Forzar -or (Test-TcpPort $Port)) {
        Kill-ProcessOnPort $Port
        Start-Sleep -Milliseconds 500
    }
}

$resultados = @()

# -------------------------------------------------------------
# 1. Reinicio de MySQL (Puerto 3306)
# -------------------------------------------------------------
if ($Servicio -in @('mysql', 'todos')) {
    Write-Host "[REINICIO] Restableciendo Base de Datos MySQL..." -ForegroundColor Cyan
    $mysqlBin = "$root\.local\mysql-8.4.11-winx64\bin"
    $myIni = "$root\.local\my.ini"
    $rootCnf = "$root\.local\root.cnf"

    if (Test-Path "$mysqlBin\mysqladmin.exe") {
        & "$mysqlBin\mysqladmin.exe" "--defaults-file=$rootCnf" shutdown 2>$null | Out-Null
    }
    Start-Sleep -Milliseconds 800
    if ($Forzar -or (Test-TcpPort 3306)) {
        Kill-ProcessOnPort 3306
    }

    if (Test-Path "$mysqlBin\mysqld.exe") {
        Start-Process "$mysqlBin\mysqld.exe" -ArgumentList "--defaults-file=`"$myIni`"" -WindowStyle Hidden
    }

    $ok = Wait-TcpPort 3306 25
    $resultados += [PSCustomObject]@{
        Servicio = 'mysql'
        Puerto = 3306
        Exito = $ok
        Mensaje = $(if ($ok) { 'Base de datos MySQL restablecida en puerto 3306.' } else { 'Fallo al iniciar MySQL en el tiempo límite.' })
    }
}

# -------------------------------------------------------------
# 2. Reinicio de Microservicio IA (Puerto 8000)
# -------------------------------------------------------------
if ($Servicio -in @('ia', 'todos')) {
    Write-Host "[REINICIO] Restableciendo Microservicio de IA..." -ForegroundColor Cyan
    Stop-RupiProcess 'ia.pid' 8000

    $iaPython = "$root\backend\service-ia\.venv\Scripts\python.exe"
    if (!(Test-Path $iaPython)) {
        $iaPython = (Get-Command python.exe -ErrorAction SilentlyContinue).Source
    }

    if ($iaPython -and (Test-Path $iaPython)) {
        $iaProc = Start-Process $iaPython `
            -ArgumentList '-m uvicorn main:app --host 127.0.0.1 --port 8000' `
            -WorkingDirectory "$root\backend\service-ia" `
            -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput "$root\.local\ia.log" `
            -RedirectStandardError "$root\.local\ia-error.log"

        $iaProc.Id | Set-Content "$root\.local\ia.pid"
        $ok = Wait-TcpPort 8000 20
        $resultados += [PSCustomObject]@{
            Servicio = 'ia'
            Puerto = 8000
            Exito = $ok
            Mensaje = $(if ($ok) { 'Microservicio de IA restablecido en puerto 8000.' } else { 'Fallo al iniciar IA en el tiempo límite.' })
        }
    } else {
        $resultados += [PSCustomObject]@{
            Servicio = 'ia'
            Puerto = 8000
            Exito = $false
            Mensaje = 'No se encontró el ejecutable de Python para iniciar el servicio de IA.'
        }
    }
}

# -------------------------------------------------------------
# 3. Reinicio de API Spring Boot (Puerto 8081)
# -------------------------------------------------------------
if ($Servicio -in @('api', 'todos')) {
    Write-Host "[REINICIO] Restableciendo API Central Spring Boot..." -ForegroundColor Cyan
    Stop-RupiProcess 'api.pid' 8081

    $jar = "$root\backend\api-spring-boot\target\rupi-api-0.1.0-SNAPSHOT.jar"
    if (!(Test-Path $jar)) {
        & mvn.cmd -f "$root\backend\api-spring-boot\pom.xml" -B package -DskipTests
    }

    $socketFallback = Join-Path $env:TEMP ('rupi-no-afunix-' + [Guid]::NewGuid().ToString('N'))
    $apiArgs = @(
        "`"-Djdk.net.unixdomain.tmpdir=$socketFallback`"",
        '-Djava.net.preferIPv4Stack=true',
        '-jar',
        "`"$jar`""
    )
    $apiProc = Start-Process (Get-Command java.exe).Source `
        -ArgumentList $apiArgs `
        -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput "$root\.local\api.log" `
        -RedirectStandardError "$root\.local\api-error.log"

    $apiProc.Id | Set-Content "$root\.local\api.pid"
    $ok = Wait-TcpPort 8081 40
    $resultados += [PSCustomObject]@{
        Servicio = 'api'
        Puerto = 8081
        Exito = $ok
        Mensaje = $(if ($ok) { 'API Spring Boot restablecida en puerto 8081.' } else { 'Fallo al iniciar la API en el tiempo límite.' })
    }
}

$stopwatch.Stop()
$tiempoTotalMs = [int]$stopwatch.ElapsedMilliseconds

$respuestaFinal = [PSCustomObject]@{
    Accion = 'REINICIO_SERVICIO'
    ServicioSolicitado = $Servicio
    TiempoTotalMs = $tiempoTotalMs
    Resultados = $resultados
    TodosExitosos = ($resultados | Where-Object { -not $_.Exito }).Count -eq 0
}

# Salida formateada JSON para consumo directo desde Django o scripts
$respuestaFinal | ConvertTo-Json -Depth 4
