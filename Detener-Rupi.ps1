$ErrorActionPreference = 'Stop'
foreach ($service in @('web', 'api')) {
    $pidFile = "$PSScriptRoot\.local\$service.pid"
    if (Test-Path $pidFile) {
        $savedId = [int](Get-Content $pidFile)
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$savedId"
        if ($process -and $process.CommandLine -like "*$PSScriptRoot*") {
            Stop-Process -Id $savedId
        }
        Remove-Item -LiteralPath $pidFile
    }
}
& "$PSScriptRoot\.local\mysql-8.4.11-winx64\bin\mysqladmin.exe" "--defaults-file=$PSScriptRoot/.local/root.cnf" shutdown
Write-Host 'Servicios de Rupi detenidos.'
