<#
.SYNOPSIS
    Instala o actualiza la tarea programada del servidor para respaldos nocturnos (HU-57).
.DESCRIPTION
    Registra la tarea `Rupi-Respaldo-Nocturno` en el Programador de Tareas de Windows
    dentro de la ventana de madrugada (00:00 a 05:59) para evitar tráfico escolar.
    Compatible con Windows PowerShell 5.1 y PowerShell 7+.
#>
[CmdletBinding()]
param(
    [string]$Hora = '02:30',
    [ValidateSet('DIARIO', 'SEMANAL')]
    [string]$Frecuencia = 'DIARIO',
    [ValidateRange(0, 6)]
    [int]$DiaSemana = 6,
    [string]$NombreTarea = 'Rupi-Respaldo-Nocturno',
    [switch]$Desinstalar
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$scriptEjecucion = Join-Path $PSScriptRoot 'Ejecutar-Respaldo-Programado.ps1'

if ($Desinstalar) {
    if (Get-Command Unregister-ScheduledTask -ErrorAction SilentlyContinue) {
        $existe = Get-ScheduledTask -TaskName $NombreTarea -ErrorAction SilentlyContinue
        if ($existe) {
            Unregister-ScheduledTask -TaskName $NombreTarea -Confirm:$false
            Write-Host "Tarea programada '$NombreTarea' eliminada."
        } else {
            Write-Host "No existe la tarea programada '$NombreTarea'."
        }
    } else {
        & schtasks.exe /Delete /TN $NombreTarea /F | Out-Null
    }
    exit 0
}

if ($Hora -notmatch '^0[0-5]:[0-5][0-9]$') {
    throw "La hora '$Hora' no pertenece a la madrugada permitida (00:00 a 05:59) para evitar horas punta escolares."
}

$psExe = (Get-Command powershell.exe -ErrorAction SilentlyContinue).Source
if (-not $psExe) {
    $psExe = (Get-Command pwsh.exe).Source
}

$argumentos = "-NoProfile -ExecutionPolicy Bypass -File `"$scriptEjecucion`""
$diasMapa = @('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')

if (Get-Command Register-ScheduledTask -ErrorAction SilentlyContinue) {
    $accion = New-ScheduledTaskAction -Execute $psExe -Argument $argumentos -WorkingDirectory $root
    if ($Frecuencia -eq 'SEMANAL') {
        $trigger = New-ScheduledTaskTrigger -Weekly -DaysOfWeek $diasMapa[$DiaSemana] -At $Hora
    } else {
        $trigger = New-ScheduledTaskTrigger -Daily -At $Hora
    }
    $config = New-ScheduledTaskSettingsSet -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Hours 2)
    Register-ScheduledTask -TaskName $NombreTarea -Action $accion -Trigger $trigger -Settings $config -Description 'Respaldo automático nocturno de la base de datos RUPI vía Django (HU-57).' -Force | Out-Null
    Write-Host "Tarea programada '$NombreTarea' registrada a las $Hora ($Frecuencia)."
} else {
    $scFreq = if ($Frecuencia -eq 'SEMANAL') { 'WEEKLY' } else { 'DAILY' }
    $cmdLine = "`"$psExe`" $argumentos"
    & schtasks.exe /Create /TN $NombreTarea /TR $cmdLine /SC $scFreq /ST $Hora /F
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo registrar la tarea '$NombreTarea' mediante schtasks.exe."
    }
}
