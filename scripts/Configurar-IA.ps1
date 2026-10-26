# Guarda la clave cifrada para el usuario Windows actual, fuera de Git.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$folder = Join-Path $root '.local'
[IO.Directory]::CreateDirectory($folder) | Out-Null
$endpoint = Read-Host 'URL HTTPS completa del endpoint Chat Completions'
$uri = $null
if (-not [Uri]::TryCreate($endpoint, [UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -ne 'https' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) { throw 'Usa una URL HTTPS sin credenciales, query ni fragmento.' }
$provider = Read-Host 'Nombre del proveedor'
$model = Read-Host 'Identificador del modelo compatible con JSON mode'
if ([string]::IsNullOrWhiteSpace($provider) -or $provider.Length -gt 80 -or [string]::IsNullOrWhiteSpace($model) -or $model.Length -gt 120) { throw 'Proveedor o modelo invalido.' }
$key = Read-Host 'Clave privada (entrada oculta)' -AsSecureString
if ($key.Length -eq 0) { throw 'No se guardo ninguna clave vacia.' }
$key | ConvertFrom-SecureString | Set-Content -LiteralPath (Join-Path $folder 'ai-key.dpapi') -Encoding UTF8
@{ endpoint = $endpoint; provider = $provider; model = $model } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $folder 'ai-config.json') -Encoding UTF8
Write-Host 'Configuracion privada guardada. Reinicia el backend para activarla. No se hizo ninguna llamada de pago.'
