param([string]$RootConfig = '', [string]$ApiUser = '')
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
if (!$RootConfig) { $RootConfig = Join-Path $project '.local/root.cnf' }
if (!$ApiUser) {
    $environmentFile = Join-Path $project '.local/env.ps1'
    if (Test-Path -LiteralPath $environmentFile) { . $environmentFile }
    $ApiUser = $env:RUPI_DB_USERNAME
    if (!$ApiUser) { $ApiUser = 'rupi' }
}
if ($ApiUser -notmatch '^[A-Za-z0-9_-]+$') { throw 'Nombre de usuario de base de datos no valido.' }
$mysql = Join-Path $project '.local/mysql-8.4.11-winx64/bin/mysql.exe'
if (!(Test-Path -LiteralPath $mysql)) { $mysql = (Get-Command mysql.exe -ErrorAction Stop).Source }
if (!(Test-Path -LiteralPath $RootConfig)) { throw 'Indica -RootConfig con la configuracion administrativa de MySQL.' }
$previousEncoding = $OutputEncoding
try {
    $OutputEncoding = New-Object System.Text.UTF8Encoding($false)
    $migration = Get-Content -LiteralPath (Join-Path $project 'database/mysql/migrations/V13__calendarizacion_ruta_aula.sql') -Raw -Encoding UTF8
    $migration | & $mysql "--defaults-extra-file=$RootConfig" --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo aplicar V13. Comprueba que MySQL este iniciado.' }
    $grants = Get-Content -LiteralPath (Join-Path $project 'database/mysql/grants-spring-boot.sql') -Raw -Encoding UTF8
    ("SET @api_usuario = '$ApiUser';`n" + $grants) | & $mysql "--defaults-extra-file=$RootConfig" --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) { throw 'V13 creada; falta otorgar permisos a la cuenta de la API.' }
    Write-Host 'HU-13: tabla y permisos preparados. No se modificaron fechas ni membresias de aulas.'
} finally { $OutputEncoding = $previousEncoding }
