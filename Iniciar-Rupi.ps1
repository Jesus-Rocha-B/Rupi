param([switch]$Recompilar)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
. "$PSScriptRoot\.local\env.ps1"
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
$env:Path = "$PSScriptRoot\.local\apache-maven-3.9.16\bin;$env:JAVA_HOME\bin;$env:Path"
function Test-Port([int]$Port) {
    $client = New-Object Net.Sockets.TcpClient
    try { $client.Connect('127.0.0.1', $Port); return $true } catch { return $false } finally { $client.Dispose() }
}
function Wait-Port([int]$Port) {
    for ($i = 0; $i -lt 60; $i++) {
        if (Test-Port $Port) { return }
        Start-Sleep -Seconds 1
    }
    throw "No inicio el puerto $Port. Revisa los logs en .local."
}
if (!(Test-Port 3307)) {
    Start-Process "$PSScriptRoot\.local\mysql-8.4.11-winx64\bin\mysqld.exe" -ArgumentList "--defaults-file=$PSScriptRoot/.local/my.ini" -WindowStyle Hidden
}
Wait-Port 3307
$jar = "$PSScriptRoot\backend\api-spring-boot\target\rupi-api-0.1.0-SNAPSHOT.jar"
if ($Recompilar -or !(Test-Path $jar)) {
    & "$PSScriptRoot\.local\apache-maven-3.9.16\bin\mvn.cmd" -f "$PSScriptRoot\backend\api-spring-boot\pom.xml" -B package
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar la API.' }
}
if (!(Test-Port 8081)) {
    $api = Start-Process "$env:JAVA_HOME\bin\java.exe" -ArgumentList "-jar `"$jar`"" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$PSScriptRoot\.local\api.log" -RedirectStandardError "$PSScriptRoot\.local\api-error.log"
    $api.Id | Set-Content "$PSScriptRoot\.local\api.pid"
}
Wait-Port 8081
if (!(Test-Port 5173)) {
    $web = Start-Process (Get-Command node.exe).Source -ArgumentList "`"$PSScriptRoot\node_modules\vite\bin\vite.js`" --host 127.0.0.1 --port 5173 --strictPort" -WorkingDirectory "$PSScriptRoot\frontend\web-react" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$PSScriptRoot\.local\web.log" -RedirectStandardError "$PSScriptRoot\.local\web-error.log"
    $web.Id | Set-Content "$PSScriptRoot\.local\web.pid"
}
Wait-Port 5173
Write-Host 'Rupi disponible en http://localhost:5173'
