$ErrorActionPreference = 'Stop'
$envFile = Join-Path (Split-Path $PSScriptRoot -Parent) '.local/env.ps1'
if (Test-Path -LiteralPath $envFile) { . $envFile }
. "$PSScriptRoot/Cargar-IA.ps1"
$jar = Join-Path (Split-Path $PSScriptRoot -Parent) 'backend/api-spring-boot/target/rupi-api-0.1.0-SNAPSHOT.jar'
$socketFallback = Join-Path $env:TEMP ('rupi-no-afunix-' + [Guid]::NewGuid().ToString('N'))
Write-Host 'Iniciando Spring Boot con configuracion privada del entorno.'
# Las claves se heredan por entorno, nunca por argumentos de proceso.
& (Get-Command java.exe).Source "-Djdk.net.unixdomain.tmpdir=$socketFallback" '-Djava.net.preferIPv4Stack=true' '-jar' $jar
