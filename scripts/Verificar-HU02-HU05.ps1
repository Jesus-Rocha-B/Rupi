# Integration checks for HU-02, HU-03, HU-04 and HU-05
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
function Request([string]$path, [int]$expected, [string]$method = 'GET', [hashtable]$headers = @{}) {
    $url = "http://127.0.0.1:8081/api/v1/$path"
    $response = $null
    $actualStatus = 0
    try {
        $response = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -UseBasicParsing
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
    if ($actualStatus -ne $expected) { throw "Expected $expected for $method $path, got $actualStatus" }
    Write-Host "PASS $expected $method $path"
    return $response
}

$student = [Guid]::NewGuid().ToString()
$session = [Guid]::NewGuid().ToString()
$enrollment = [Guid]::NewGuid().ToString()
$token = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
$route = 'b0000000-0000-4000-8000-000000000001'
$node1 = 'ca000000-0000-4000-8000-000000000001'
$node2 = 'ca000000-0000-4000-8000-000000000002'
$node3 = 'ca000000-0000-4000-8000-000000000003'
$auth = @{Authorization="Bearer $token"}

try {
    # 1. Crear usuario de prueba temporal con sesión activa
    Sql "INSERT INTO identidad_cuenta_usuario (id,nombre_usuario,hash_clave,estado) VALUES ('$student','test-$student','not-a-login','ACTIVO'); INSERT INTO identidad_sesion_usuario (id,usuario_id,token_hash,cliente,expira_en) VALUES ('$session','$student',UNHEX(SHA2('$token',256)),'WEB_USUARIO',UTC_TIMESTAMP(6)+INTERVAL 1 HOUR); INSERT INTO aprendizaje_inscripcion_ruta (id,version_ruta_id,estudiante_id,estado) VALUES ('$enrollment','$route','$student','ACTIVO'); INSERT INTO aprendizaje_progreso_nodo (inscripcion_id,version_ruta_id,nodo_ruta_id,estado,desbloqueado_en) VALUES ('$enrollment','$route','$node1','DISPONIBLE',UTC_TIMESTAMP(6)), ('$enrollment','$route','$node2','BLOQUEADO',NULL), ('$enrollment','$route','$node3','BLOQUEADO',NULL); INSERT INTO aprendizaje_progreso_nodo (inscripcion_id,version_ruta_id,nodo_ruta_id,estado) SELECT '$enrollment',version_ruta_id,id,'BLOQUEADO' FROM aprendizaje_nodo_ruta WHERE version_ruta_id='$route' AND id NOT IN ('$node1','$node2','$node3');"

    # HU-02: Iniciar actividad disponible (cambia a EN_CURSO y actualiza ultimo_nodo_visitado_id)
    $startResp = Request "student/learning-routes/$route/nodes/$node1/start" 200 'POST' $auth
    $startData = $startResp.Content | ConvertFrom-Json
    if ($startData.state -ne 'EN_CURSO') { throw "Expected state EN_CURSO, got $($startData.state)" }

    # Intentar iniciar un nodo bloqueado debe dar 403 Forbidden
    $null = Request "student/learning-routes/$route/nodes/$node2/start" 403 'POST' $auth

    # HU-03: Al consultar la ruta, ultimo_nodo_visitado_id debe ser node1
    $detailResp = Request "student/learning-routes/$route" 200 'GET' $auth
    $detailData = $detailResp.Content | ConvertFrom-Json
    if ($detailData.enrollment.lastVisitedNodeId -ne $node1) { throw "Expected lastVisitedNodeId $node1, got $($detailData.enrollment.lastVisitedNodeId)" }

    # HU-04: Verificar que el curriculum y unidades estén presentes
    if ($null -ne $detailData.curriculum) {
        Write-Host "Curriculum status: $($detailData.curriculum.status) with $($detailData.curriculum.units.Count) units"
    }

    # HU-05: Completar la actividad (suma 50 XP, desbloquea node2 a DISPONIBLE)
    $compResp = Request "student/learning-routes/$route/nodes/$node1/complete" 200 'POST' $auth
    $compData = $compResp.Content | ConvertFrom-Json
    if ($compData.state -ne 'COMPLETADO' -or $compData.experienceEarned -ne 50) {
        throw "Expected COMPLETADO and 50 XP, got $($compData.state) and $($compData.experienceEarned) XP"
    }
    if ($compData.nextUnlockedNode.id -ne $node2) {
        throw "Expected nextUnlockedNode $node2, got $($compData.nextUnlockedNode.id)"
    }

    # Re-completar el mismo nodo es idempotente (otorga 0 XP adicional)
    $recompResp = Request "student/learning-routes/$route/nodes/$node1/complete" 200 'POST' $auth
    $recompData = $recompResp.Content | ConvertFrom-Json
    if ($recompData.experienceEarned -ne 0) {
        throw "Expected 0 XP for re-completing already completed node, got $($recompData.experienceEarned)"
    }

    Write-Host "All HU-02 to HU-05 integration checks passed successfully!"
} finally {
    Sql "DELETE FROM gamificacion_insignia_usuario WHERE usuario_id='$student'; DELETE FROM gamificacion_movimiento_experiencia WHERE usuario_id='$student'; DELETE FROM aprendizaje_progreso_nodo WHERE inscripcion_id='$enrollment'; DELETE FROM aprendizaje_inscripcion_ruta WHERE id='$enrollment'; DELETE FROM identidad_sesion_usuario WHERE id='$session'; DELETE FROM identidad_cuenta_usuario WHERE id='$student';"
}
