$ErrorActionPreference = 'Stop'
$envFile = "$PSScriptRoot\..\.local\env.ps1"
if (Test-Path $envFile) {
    . $envFile
}
$jar = "$PSScriptRoot\..\backend\api-spring-boot\target\rupi-api-0.1.0-SNAPSHOT.jar"

Write-Host "Iniciando Spring Boot Rupi API con DB: $env:RUPI_DB_URL"

& (Get-Command java.exe).Source `
    "-Djdk.net.unixdomain.tmpdir=$PSScriptRoot/../.local/sockets" `
    "-Djava.net.preferIPv4Stack=true" `
    "-Dspring.datasource.url=$env:RUPI_DB_URL" `
    "-Dspring.datasource.username=$env:RUPI_DB_USERNAME" `
    "-Dspring.datasource.password=$env:RUPI_DB_PASSWORD" `
    "-jar" `
    $jar
