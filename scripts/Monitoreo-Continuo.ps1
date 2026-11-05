<#
.SYNOPSIS
    Herramienta de monitoreo continuo y vigilancia activa de infraestructura (HU-47).
.DESCRIPTION
    Sondea continuamente la Base de Datos MySQL y el Servicio de IA (FastAPI/Gemini).
    Ante caídas detectadas, dispara alertas automáticas y ejecuta scripts de reinicio de emergencia.
    Compatible con Windows PowerShell 5.1 y PowerShell 7+.
.PARAMETER IntervaloSegundos
    Tiempo entre ciclos de sondeo en segundos (por defecto: 15 segundos).
.PARAMETER UnaVez
    Ejecuta un solo ciclo de diagnóstico y finaliza (útil para auditorías o pruebas).
.PARAMETER SinAutoReinicio
    Desactiva la ejecución automática de reinicio de emergencia tras una caída.
#>
[CmdletBinding()]
param(
    [int]$IntervaloSegundos = 15,
    [switch]$UnaVez,
    [switch]$SinAutoReinicio
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path "$PSScriptRoot\.."

# Cargar variables de entorno locales si existen
$envFile = "$root\.local\env.ps1"
if (Test-Path $envFile) {
    . $envFile
}

function Test-ServicePort([string]$HostName = '127.0.0.1', [int]$Port, [int]$TimeoutMs = 1500) {
    $client = New-Object Net.Sockets.TcpClient
    try {
        $ar = $client.BeginConnect($HostName, $Port, $null, $null)
        $success = $ar.AsyncWaitHandle.WaitOne($TimeoutMs, $false)
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

function Invoke-DjangoIncidentHook([string]$Servicio, [string]$Estado, [string]$Detalle) {
    try {
        # Si Django está activo en 8082, notificar el evento de monitoreo
        $body = @{
            servicio = $Servicio
            estado = $Estado
            detalle = $Detalle
        } | ConvertTo-Json

        $headers = @{
            "Content-Type" = "application/json"
        }
        if ($env:RUPI_MONITOR_TOKEN) {
            $headers["X-Rupi-Monitor-Token"] = $env:RUPI_MONITOR_TOKEN
        }

        $res = Invoke-RestMethod -Uri "http://127.0.0.1:8082/api/admin/v1/monitoreo/evento-externo" `
            -Method Post `
            -Headers $headers `
            -Body $body `
            -TimeoutSec 3 `
            -ErrorAction SilentlyContinue
    } catch {
        # Django puede estar procesando o reiniciándose
    }
}

function Ejecutar-CicloMonitoreo {
    $fecha = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    Write-Host "`n[$fecha] --- Ciclo de Monitoreo Continuo Rupi ---" -ForegroundColor DarkCyan

    $dbHost = if ($env:RUPI_ADMIN_DB_HOST) { $env:RUPI_ADMIN_DB_HOST } else { '127.0.0.1' }
    $dbPort = if ($env:RUPI_ADMIN_DB_PORT) { [int]$env:RUPI_ADMIN_DB_PORT } else { 3306 }
    $dbNombre = if ($dbHost -in @('127.0.0.1', 'localhost', '::1')) { "Base de Datos MySQL (Puerto $dbPort)" } else { "Base de Datos Cloud ($($dbHost):$($dbPort))" }

    $servicios = @(
        @{ Clave = 'mysql'; Nombre = $dbNombre; Host = $dbHost; Puerto = $dbPort; Critico = $true },
        @{ Clave = 'ia'; Nombre = 'Servicio de IA (FastAPI)'; Host = '127.0.0.1'; Puerto = 8000; Critico = $true },
        @{ Clave = 'api'; Nombre = 'API Spring Boot'; Host = '127.0.0.1'; Puerto = 8081; Critico = $true }
    )

    $caidos = @()

    foreach ($s in $servicios) {
        $ok = Test-ServicePort -HostName $s.Host -Port $s.Puerto
        if ($ok) {
            Write-Host "  [OK] $($s.Nombre) respondiendo en $($s.Host):$($s.Puerto)." -ForegroundColor Green
        } else {
            Write-Host "  [ALERTA CAIDA] $($s.Nombre) no responde en $($s.Host):$($s.Puerto)!" -ForegroundColor Red
            $caidos += $s
            Invoke-DjangoIncidentHook -Servicio $s.Clave -Estado 'CAIDO' -Detalle "$($s.Host):$($s.Puerto) inaccesible"
        }
    }

    if ($caidos.Count -gt 0) {
        Write-Warning "Se detectaron $($caidos.Count) servicio(s) caído(s)."
        if (-not $SinAutoReinicio) {
            foreach ($c in $caidos) {
                Write-Host "  -> Aplicando medida correctiva de emergencia: reiniciando $($c.Nombre)..." -ForegroundColor Yellow
                try {
                    & "$PSScriptRoot\Reiniciar-Servicio.ps1" -Servicio $c.Clave -Forzar
                } catch {
                    Write-Error "Fallo al ejecutar el autoreinicio para $($c.Nombre): $_"
                }
            }
        }
    } else {
        Write-Host "  Estado general de infraestructura: OPERATIVO." -ForegroundColor Cyan
    }
}

Write-Host "Iniciando Watchdog de Monitoreo Continuo RUPI (Intervalo: ${IntervaloSegundos}s)..." -ForegroundColor Magenta

if ($UnaVez) {
    Ejecutar-CicloMonitoreo
    exit 0
}

while ($true) {
    Ejecutar-CicloMonitoreo
    Start-Sleep -Seconds $IntervaloSegundos
}
