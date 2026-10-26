# Dot-source: no imprime ni coloca claves en argumentos de procesos.
$aiFolder = Join-Path (Split-Path $PSScriptRoot -Parent) '.local'
$aiConfigPath = Join-Path $aiFolder 'ai-config.json'
$aiKeyPath = Join-Path $aiFolder 'ai-key.dpapi'
if ((Test-Path -LiteralPath $aiConfigPath) -and (Test-Path -LiteralPath $aiKeyPath)) {
    $aiConfig = Get-Content -LiteralPath $aiConfigPath -Raw | ConvertFrom-Json
    $aiSecure = (Get-Content -LiteralPath $aiKeyPath -Raw).Trim() | ConvertTo-SecureString -ErrorAction Stop
    $aiPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($aiSecure)
    try { $env:RUPI_AI_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($aiPointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($aiPointer) }
    $env:RUPI_AI_ENDPOINT = $aiConfig.endpoint
    $env:RUPI_AI_PROVIDER = $aiConfig.provider
    $env:RUPI_AI_MODEL = $aiConfig.model
    Remove-Variable aiConfig,aiSecure,aiPointer -ErrorAction SilentlyContinue
}
