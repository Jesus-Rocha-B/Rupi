# Integration check for HU-08: Culminación de Ruta y Diploma de Logro Escolar (Gallito de las Rocas)
# Compatible con Windows PowerShell 5.1 y PowerShell 7+
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
if (Test-Path "$root\.local\env.ps1") {
    . "$root\.local\env.ps1"
}
$mysql = "$root\.local\mysql-8.4.11-winx64\bin\mysql.exe"
function Sql([string]$query) {
    if (Test-Path $mysql) {
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
    } catch [System.Net.WebException] {
        if ($_.Exception.Response) {
            $webResp = [System.Net.HttpWebResponse]$_.Exception.Response
            $actualStatus = [int]$webResp.StatusCode
            $content = $null
            $stream = $webResp.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $content = $reader.ReadToEnd()
            }
            $respHeaders = @{}
            foreach ($k in $webResp.Headers.AllKeys) {
                $respHeaders[$k] = $webResp.Headers[$k]
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
$node10 = 'ca000000-0000-4000-8000-000000000010'
$auth = @{Authorization="Bearer $token"}

Write-Host "== Instando verificación HU-08: Diploma y Graduación Tunqui =="
try {
    # 1. Crear estudiante de prueba y su sesión
    Sql "INSERT INTO identidad_usuario (id, tipo_usuario, nombre_completo, estado, creado_en, actualizado_en) VALUES ('$student', 'ESTUDIANTE', 'Killa Quispe', 'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));"
    Sql "INSERT INTO identidad_estudiante (usuario_id, codigo_estudiante, fecha_nacimiento, nivel_educativo, grado, seccion) VALUES ('$student', 'STU-HU08', '2017-05-12', 'PRIMARIA', 2, 'B');"
    Sql "INSERT INTO identidad_sesion (id, usuario_id, token_hash, creado_en, expira_en) VALUES ('$session', '$student', SHA2('$token', 256), UTC_TIMESTAMP(6), DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 2 HOUR));"

    # 2. Inscribir al estudiante en la ruta oficial de Ayacucho
    Sql "INSERT INTO aprendizaje_inscripcion_ruta (id, estudiante_id, version_ruta_id, estado, iniciado_en) VALUES ('$enrollment', '$student', '$route', 'ACTIVO', UTC_TIMESTAMP(6));"

    # 3. Marcar nodos 1 al 9 como completados
    for ($i = 1; $i -le 9; $i++) {
        $seqStr = "{0:D2}" -f $i
        $nodeId = "ca000000-0000-4000-8000-0000000000$seqStr"
        Sql "INSERT INTO aprendizaje_progreso_nodo (id, inscripcion_id, version_ruta_id, nodo_ruta_id, estado, completado_en) VALUES (UUID(), '$enrollment', '$route', '$nodeId', 'COMPLETADO', UTC_TIMESTAMP(6));"
    }
    # Y nodo 10 como DISPONIBLE
    Sql "INSERT INTO aprendizaje_progreso_nodo (id, inscripcion_id, version_ruta_id, nodo_ruta_id, estado, primer_acceso_en) VALUES (UUID(), '$enrollment', '$route', '$node10', 'DISPONIBLE', UTC_TIMESTAMP(6));"

    # 4. Iniciar y completar la última parada (Nodo 10)
    $startResp = Request "student/learning-routes/$route/nodes/$node10/start" 200 'POST' $auth
    $completeResp = Request "student/learning-routes/$route/nodes/$node10/complete" 200 'POST' $auth

    $completeJson = $completeResp.Content | ConvertFrom-Json
    if (-not $completeJson.isRouteCompleted) {
        throw "La ruta debía reportarse como completada al finalizar el nodo 10"
    }
    Write-Host "OK: isRouteCompleted = true"

    if ($completeJson.routeBonusExperience -ne 100) {
        throw "El bono de graduación debía ser de 100 XP, recibido: $($completeJson.routeBonusExperience)"
    }
    Write-Host "OK: routeBonusExperience = 100 XP"

    if ($completeJson.badgeEarned.code -ne 'GRAN_TUNQUI_ORO') {
        throw "La insignia obtenida debía ser GRAN_TUNQUI_ORO, recibida: $($completeJson.badgeEarned.code)"
    }
    Write-Host "OK: badgeEarned = GRAN_TUNQUI_ORO ($($completeJson.badgeEarned.name))"

    # 5. Consultar el endpoint del diploma oficial
    $diplomaResp = Request "student/learning-routes/$route/diploma" 200 'GET' $auth
    $diplomaJson = $diplomaResp.Content | ConvertFrom-Json

    if ($diplomaJson.studentName -ne 'Killa Quispe') {
        throw "Nombre de estudiante en diploma incorrecto: $($diplomaJson.studentName)"
    }
    if ($diplomaJson.totalCompletedNodes -ne 10 -or $diplomaJson.totalNodes -ne 10) {
        throw "Nodos completados deben ser 10/10, recibido: $($diplomaJson.totalCompletedNodes)/$($diplomaJson.totalNodes)"
    }
    if ($diplomaJson.graduationBadge.code -ne 'GRAN_TUNQUI_ORO') {
        throw "Insignia de graduación en diploma incorrecta"
    }
    if (-not $diplomaJson.rupiHonorMessage -or -not $diplomaJson.competencies) {
        throw "Falta mensaje de honor de Rupi o listado de competencias oficiales CNEB"
    }
    Write-Host "OK: Diploma escolar validado con éxito. Mensaje de Rupi: $($diplomaJson.rupiHonorMessage)"
    Write-Host "== HU-08 COMPLETADA Y VERIFICADA AL 100% =="
} finally {
    # Limpieza de datos de prueba temporales
    Sql "DELETE FROM gamificacion_insignia_usuario WHERE usuario_id = '$student';"
    Sql "DELETE FROM gamificacion_movimiento_experiencia WHERE usuario_id = '$student';"
    Sql "DELETE FROM aprendizaje_progreso_nodo WHERE inscripcion_id = '$enrollment';"
    Sql "DELETE FROM aprendizaje_inscripcion_ruta WHERE id = '$enrollment';"
    Sql "DELETE FROM identidad_sesion WHERE usuario_id = '$student';"
    Sql "DELETE FROM identidad_estudiante WHERE usuario_id = '$student';"
    Sql "DELETE FROM identidad_usuario WHERE id = '$student';"
}
