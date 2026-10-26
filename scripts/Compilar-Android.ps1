param([switch]$Lint)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
$previousAndroidHome = $env:ANDROID_HOME
try {
    if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
    if (-not (Test-Path -LiteralPath $env:ANDROID_HOME)) { throw 'Instala el SDK de Android o configura ANDROID_HOME.' }
    # Windows MSIX puede fallar al conectar AF_UNIX. Una ruta inexistente hace que
    # PipeImpl use su fallback TCP. No crear esta carpeta; no cambia ajustes del sistema.
    $socketFallback = Join-Path $env:TEMP ('rupi-no-afunix-' + [Guid]::NewGuid().ToString('N'))
    $env:JAVA_TOOL_OPTIONS = ($previousJavaOptions + ' -Djdk.net.unixdomain.tmpdir="' + $socketFallback + '" -Djava.net.preferIPv4Stack=true').Trim()
    Push-Location (Join-Path $root 'frontend/mobile-kotlin')
    try {
        $tasks = @(':app:assembleDebug')
        if ($Lint) { $tasks += ':app:lintDebug' }
        & .\gradlew.bat @tasks --no-daemon
        if ($LASTEXITCODE -ne 0) { throw 'La compilacion Android fallo. Revisa el diagnostico anterior.' }
    } finally { Pop-Location }
    Write-Host 'APK: frontend/mobile-kotlin/app/build/outputs/apk/debug/app-debug.apk'
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
    $env:ANDROID_HOME = $previousAndroidHome
}
