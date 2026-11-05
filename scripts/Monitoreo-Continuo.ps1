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

function Test-ServicePort([int]$Port, [int]$TimeoutMs = 1200) {
    $client = New-Object Net.Sockets.TcpClient
    try {
        $ar = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
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

    $servicios = @(
        @{ Clave = 'mysql'; Nombre = 'Base de Datos MySQL'; Puerto = 3306; Critico = $true },
        @{ Clave = 'ia'; Nombre = 'Servicio de IA (FastAPI)'; Puerto = 8000; Critico = $true },
        @{ Clave = 'api'; Nombre = 'API Spring Boot'; Puerto = 8081; Critico = $true }
    )

    $caidos = @()

    foreach ($s in $servicios) {
        $ok = Test-ServicePort -Port $s.Puerto
        if ($ok) {
            Write-Host "  [OK] $($s.Nombre) respondiendo en puerto $($s.Puerto)." -ForegroundColor Green
        } else {
            Write-Host "  [ALERTA CAIDA] $($s.Nombre) no responde en puerto $($s.Puerto)!" -ForegroundColor Red
            $caidos += $s
            Invoke-DjangoIncidentHook -Servicio $s.Clave -Estado 'CAIDO' -Detalle "Puerto $($s.Puerto) inaccesible"
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
