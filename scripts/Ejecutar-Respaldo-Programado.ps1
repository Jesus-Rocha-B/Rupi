<#
.SYNOPSIS
    Ejecuta la tarea programada nocturna de respaldo automático en Django (HU-57).
.DESCRIPTION
    Carga las variables de entorno privadas (.local/env.ps1) y ejecuta
    `manage.py ejecutar_respaldo_programado` para respaldar la base principal
    fuera de horario escolar hacia el almacenamiento externo seguro.
    Compatible con Windows PowerShell 5.1 y PowerShell 7+.
#>
[CmdletBinding()]
param(
    [switch]$Forzar
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$djangoDir = Join-Path $root 'backend\admin-django'

$envFile = Join-Path $root '.local\env.ps1'
if (Test-Path -LiteralPath $envFile) {
    . $envFile
}

$pythonBin = Join-Path $djangoDir '.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $pythonBin)) {
    $cmd = Get-Command python.exe -ErrorAction SilentlyContinue
    if (-not $cmd) {
        $cmd = Get-Command python -ErrorAction SilentlyContinue
    }
    if (-not $cmd) {
        throw 'No se encontro Python para ejecutar la tarea de respaldo en Django.'
    }
    $pythonBin = $cmd.Source
}

$argsList = @('manage.py', 'ejecutar_respaldo_programado')
if ($Forzar) {
    $argsList += '--forzar'
} else {
    $argsList += '--verificar-ventana'
}

Push-Location $djangoDir
try {
    & $pythonBin @argsList
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($exitCode -ne 0) {
    exit $exitCode
}
exit 0
