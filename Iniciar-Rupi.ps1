param([switch]$Recompilar)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
. "$PSScriptRoot\.local\env.ps1"
. "$PSScriptRoot\scripts\Cargar-IA.ps1"
New-Item -ItemType Directory -Force "$PSScriptRoot/.local/sockets" | Out-Null
# HU-45: la API y la consola comparten el token del monitor; se guarda en .local para que sigan coincidiendo.
if (!$env:RUPI_MONITOR_TOKEN) {
    $monitorFile = "$PSScriptRoot\.local\monitor.token"
    if (!(Test-Path $monitorFile)) {
        ([Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')) | Set-Content -NoNewline -Encoding ASCII $monitorFile
    }
    $env:RUPI_MONITOR_TOKEN = (Get-Content -Raw $monitorFile).Trim()
}

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
    $socketFallback = Join-Path $env:TEMP ('rupi-no-afunix-' + [Guid]::NewGuid().ToString('N'))
    $apiArgs = @(
        "`"-Djdk.net.unixdomain.tmpdir=$socketFallback`"",
        '-Djava.net.preferIPv4Stack=true',
        '-jar',
        "`"$jar`""
    )
    $api = Start-Process (Get-Command java.exe).Source -ArgumentList $apiArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput "$PSScriptRoot\.local\api.log" -RedirectStandardError "$PSScriptRoot\.local\api-error.log"
    $api.Id | Set-Content "$PSScriptRoot\.local\api.pid"
}
Wait-Port 8081
# API administrativa Django (HU-41 a HU-46). Requiere el entorno de backend\admin-django\.venv o python del sistema.
$adminPython = "$PSScriptRoot\backend\admin-django\.venv\Scripts\python.exe"
if (!(Test-Path $adminPython)) {
    $adminPython = (Get-Command python.exe -ErrorAction SilentlyContinue).Source
}
if (!(Test-Port 8082)) {
    if ($adminPython -and (Test-Path $adminPython)) {
        if (!$env:RUPI_ADMIN_DB_PORT) { $env:RUPI_ADMIN_DB_PORT = "$mysqlPort" }
        # Django usa su propio usuario MySQL (grants-django-admin.sql), nunca el de Spring: cada servicio escribe solo lo suyo.
        if (!$env:RUPI_ADMIN_DB_PASSWORD) { Write-Warning 'Falta RUPI_ADMIN_DB_PASSWORD en .local\env.ps1: la consola /admin no podra conectarse a MySQL.' }
        $admin = Start-Process $adminPython -ArgumentList 'manage.py runserver 127.0.0.1:8082 --noreload' -WorkingDirectory "$PSScriptRoot\backend\admin-django" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$PSScriptRoot\.local\admin.log" -RedirectStandardError "$PSScriptRoot\.local\admin-error.log"
        $admin.Id | Set-Content "$PSScriptRoot\.local\admin.pid"
        Wait-Port 8082
    } else {
        Write-Warning 'Sin entorno de Django: la consola /admin no estara disponible. Ver backend\admin-django\README.md.'
    }
}
# Microservicio de IA Educativa Rupi (FastAPI en puerto 8000)
$iaPython = "$PSScriptRoot\backend\service-ia\.venv\Scripts\python.exe"
if (!(Test-Path $iaPython)) {
    $iaPython = (Get-Command python.exe -ErrorAction SilentlyContinue).Source
}
if (!(Test-Port 8000)) {
    if ($iaPython -and (Test-Path $iaPython)) {
        $ia = Start-Process $iaPython -ArgumentList '-m uvicorn main:app --host 127.0.0.1 --port 8000' -WorkingDirectory "$PSScriptRoot\backend\service-ia" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$PSScriptRoot\.local\ia.log" -RedirectStandardError "$PSScriptRoot\.local\ia-error.log"
        $ia.Id | Set-Content "$PSScriptRoot\.local\ia.pid"
        Wait-Port 8000
    } else {
        Write-Warning 'Sin entorno de Python para el microservicio de IA. Ver backend\service-ia\README.md.'
    }
}
if (!(Test-Port 5173)) {
    $web = Start-Process (Get-Command node.exe).Source -ArgumentList "`"$PSScriptRoot\node_modules\vite\bin\vite.js`" --host 127.0.0.1 --port 5173 --strictPort" -WorkingDirectory "$PSScriptRoot\frontend\web-react" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$PSScriptRoot\.local\web.log" -RedirectStandardError "$PSScriptRoot\.local\web-error.log"
    $web.Id | Set-Content "$PSScriptRoot\.local\web.pid"
}
Wait-Port 5173
Write-Host 'Rupi disponible en http://localhost:5173'
Write-Host 'Consola de Administracion: http://localhost:5173/admin'
Write-Host 'Microservicio de IA activo en http://127.0.0.1:8000'
