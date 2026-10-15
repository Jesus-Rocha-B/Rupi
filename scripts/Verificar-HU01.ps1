# Integration checks against the isolated local development database.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$envFile = Join-Path (Join-Path $root '.local') 'env.ps1'
if (Test-Path $envFile) {
    . $envFile
}
$mysqlDir = Get-ChildItem -LiteralPath (Join-Path $root '.local') -Directory -Filter 'mysql-8.4*' -ErrorAction SilentlyContinue | Select-Object -First 1
$mysqlExe = if ($PSVersionTable.PSVersion.Major -lt 6 -or $IsWindows) { 'mysql.exe' } else { 'mysql' }
$mysql = if ($mysqlDir) { Join-Path (Join-Path $mysqlDir.FullName 'bin') $mysqlExe } else { $null }
function Sql([string]$query) {
    if ($mysql -and (Test-Path $mysql)) {
        $query | & $mysql "--defaults-file=$root/.local/root.cnf" --database=rupi --batch
        if ($LASTEXITCODE -ne 0) { throw 'SQL verification failed' }
    }
}
function Request([string]$path, [int]$expected, [hashtable]$headers = @{}) {
    $url = "http://127.0.0.1:8081/api/v1/$path"
    $response = $null
    $actualStatus = 0
    try {
        $response = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing
        $actualStatus = [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            $webResp = $_.Exception.Response
            $actualStatus = [int]$webResp.StatusCode
            $content = $null
            $respHeaders = @{}
            if ($PSVersionTable.PSVersion.Major -ge 6) {
                $content = $_.ErrorDetails.Message
                foreach ($h in $webResp.Headers) { $respHeaders[$h.Key] = ($h.Value -join ', ') }
            } else {
                $stream = $webResp.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $content = $reader.ReadToEnd()
                }
                foreach ($k in $webResp.Headers.AllKeys) { $respHeaders[$k] = $webResp.Headers[$k] }
            }
            $response = [PSCustomObject]@{
                StatusCode = $actualStatus
                Content = $content
                Headers = $respHeaders
            }
        } else {
            throw $_
        }
    }
    if ($actualStatus -ne $expected) { throw "Expected $expected for $path, got $actualStatus" }
    Write-Host "PASS $expected $path"
    return $response
}
$student = [Guid]::NewGuid().ToString()
$session = [Guid]::NewGuid().ToString()
$enrollment = [Guid]::NewGuid().ToString()
$token = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
$route = 'b0000000-0000-4000-8000-000000000001'
$auth = @{Authorization="Bearer $token"}
try {
    $null = Request 'student/learning-routes' 401
    $null = Request 'student/learning-routes' 401 @{Authorization='Bearer 11111111-1111-4111-8111-111111111111'}
    $null = Request 'student/learning-routes' 401 @{'X-Student-Id'='11111111-1111-4111-8111-111111111111'}
    Sql "INSERT INTO identidad_cuenta_usuario (id,nombre_usuario,hash_clave,estado) VALUES ('$student','test-$student','not-a-login','ACTIVO'); INSERT INTO identidad_sesion_usuario (id,usuario_id,token_hash,cliente,expira_en) VALUES ('$session','$student',UNHEX(SHA2('$token',256)),'WEB_USUARIO',UTC_TIMESTAMP(6)+INTERVAL 1 HOUR);"
    $response = Request 'student/learning-routes' 200 $auth
    if (($response.Content | ConvertFrom-Json).items.Count -ne 0) { throw 'Expected empty routes' }
    $null = Request "student/learning-routes/$route" 404 $auth
    Sql "INSERT INTO aprendizaje_inscripcion_ruta (id,version_ruta_id,estudiante_id,estado) VALUES ('$enrollment','$route','$student','ACTIVO');"
    # Una inscripción sin filas de progreso ya no es un error (503): la primera parada se muestra DISPONIBLE y la API
    # crea las filas que falten al abrir o completar una actividad (V17 repara las inscripciones antiguas).
    $response = Request "student/learning-routes/$route" 200 $auth
    $first = (($response.Content | ConvertFrom-Json).nodes | Sort-Object sequence | Select-Object -First 1)
    if ($first.state -ne 'DISPONIBLE') { throw "Expected first node DISPONIBLE without progress rows, got $($first.state)" }
    Sql "INSERT INTO aprendizaje_progreso_nodo (inscripcion_id,version_ruta_id,nodo_ruta_id,estado) SELECT '$enrollment',version_ruta_id,id,'BLOQUEADO' FROM aprendizaje_nodo_ruta WHERE version_ruta_id='$route';"
    $response = Request "student/learning-routes/$route" 200 $auth
    $detail = $response.Content | ConvertFrom-Json
    if ($detail.nodes.Count -ne 10 -or $detail.progress.completedNodes -ne 0) { throw 'Progress leaked from another student' }
    if ((@($response.Headers['Cache-Control']) -join ', ') -notmatch 'no-store') { throw 'Private response must not be cached' }
    Sql "UPDATE aprendizaje_inscripcion_ruta SET estado='SUSPENDIDO' WHERE id='$enrollment';"
    $null = Request "student/learning-routes/$route" 404 $auth
    Sql "UPDATE identidad_sesion_usuario SET creada_en=UTC_TIMESTAMP(6)-INTERVAL 2 HOUR, expira_en=UTC_TIMESTAMP(6)-INTERVAL 1 HOUR WHERE id='$session';"
    $null = Request 'student/learning-routes' 401 $auth
    Sql "UPDATE identidad_sesion_usuario SET expira_en=UTC_TIMESTAMP(6)+INTERVAL 1 HOUR, revocada_en=UTC_TIMESTAMP(6) WHERE id='$session';"
    $null = Request 'student/learning-routes' 401 $auth
    Write-Host 'All HU-01 integration checks passed.'
} finally {
    Sql "DELETE FROM aprendizaje_progreso_nodo WHERE inscripcion_id='$enrollment'; DELETE FROM aprendizaje_inscripcion_ruta WHERE id='$enrollment'; DELETE FROM identidad_sesion_usuario WHERE id='$session'; DELETE FROM identidad_cuenta_usuario WHERE id='$student';"
}
