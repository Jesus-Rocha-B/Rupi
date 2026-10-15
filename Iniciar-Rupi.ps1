param([switch]$Recompilar)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
. "$PSScriptRoot\.local\env.ps1"
New-Item -ItemType Directory -Force "$PSScriptRoot/.local/sockets" | Out-Null

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
$mysqlPort = 3307
if (Test-Port 3306) {
    $mysqlPort = 3306
} elseif (!(Test-Port 3307)) {
    if (Test-Path "$PSScriptRoot\.local\mysql-8.4.11-winx64\bin\mysqld.exe") {
        Start-Process "$PSScriptRoot\.local\mysql-8.4.11-winx64\bin\mysqld.exe" -ArgumentList "--defaults-file=`"$PSScriptRoot/.local/my.ini`"" -WindowStyle Hidden
    }
}
Wait-Port $mysqlPort
$jar = "$PSScriptRoot\backend\api-spring-boot\target\rupi-api-0.1.0-SNAPSHOT.jar"
if ($Recompilar -or !(Test-Path $jar)) {
    & mvn.cmd -f "$PSScriptRoot\backend\api-spring-boot\pom.xml" -B package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar la API.' }
}
if (!(Test-Port 8081)) {
    # El proceso hereda las variables de entorno; no colocar secretos en sus argumentos.
    # Start-Process concatena ArgumentList: las rutas con espacios deben ir entre comillas.
    $apiArgs = @(
        "`"-Djdk.net.unixdomain.tmpdir=$PSScriptRoot/.local/sockets`"",
        '-Djava.net.preferIPv4Stack=true',
        '-jar',
        "`"$jar`""
    )
    $api = Start-Process (Get-Command java.exe).Source -ArgumentList $apiArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput "$PSScriptRoot\.local\api.log" -RedirectStandardError "$PSScriptRoot\.local\api-error.log"
    $api.Id | Set-Content "$PSScriptRoot\.local\api.pid"
}
Wait-Port 8081
if (!(Test-Port 5173)) {
    $web = Start-Process (Get-Command node.exe).Source -ArgumentList "`"$PSScriptRoot\node_modules\vite\bin\vite.js`" --host 127.0.0.1 --port 5173 --strictPort" -WorkingDirectory "$PSScriptRoot\frontend\web-react" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$PSScriptRoot\.local\web.log" -RedirectStandardError "$PSScriptRoot\.local\web-error.log"
    $web.Id | Set-Content "$PSScriptRoot\.local\web.pid"
}
Wait-Port 5173
Write-Host 'Rupi disponible en http://localhost:5173'
