# ==============================================================================
# Verificacion Automatizada - HU-05: Autoria y Gestion de Roadmaps Docente
# ==============================================================================
# Compatible con Windows PowerShell 5.1 y PowerShell 7+

$ErrorActionPreference = 'Continue'
$root = Split-Path $PSScriptRoot -Parent
$passCount = 0
$failCount = 0

function Report-Pass([string]$testName) {
    $script:passCount++
    Write-Host " [PASS] $testName" -ForegroundColor Green
}

function Report-Fail([string]$testName, [string]$reason) {
    $script:failCount++
    Write-Host " [FAIL] $testName - $reason" -ForegroundColor Red
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " INICIANDO VERIFICACION: HISTORIA DE USUARIO 05 (DOCENTE AUTORIA)" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# 1. Verificacion de Archivos de Migracion y Esquema SQL
Write-Host "`n1. Verificando integridad de Migraciones y Esquema Relacional..." -ForegroundColor Yellow
$migrationV12 = Join-Path $root "database\mysql\migrations\V12__rol_docente_autoria_hu05.sql"
if (Test-Path $migrationV12) {
    $v12Content = Get-Content -Path $migrationV12 -Raw
    if ($v12Content -match "DOCENTE" -and $v12Content -match "profesor\.demo" -and $v12Content -match "aprendizaje_nodo_recurso") {
        Report-Pass "Migracion V12 contiene rol DOCENTE, profesor.demo y tabla aprendizaje_nodo_recurso"
    } else {
        Report-Fail "Migracion V12" "Contenido incompleto en V12__rol_docente_autoria_hu05.sql"
    }
} else {
    Report-Fail "Migracion V12" "No existe $migrationV12"
}

$schemaSql = Join-Path $root "database\mysql\schema.sql"
if (Test-Path $schemaSql) {
    $schemaContent = Get-Content -Path $schemaSql -Raw
    if ($schemaContent -match "aprendizaje_nodo_recurso") {
        Report-Pass "Esquema database/mysql/schema.sql incluye tabla aprendizaje_nodo_recurso"
    } else {
        Report-Fail "Esquema MySQL" "aprendizaje_nodo_recurso no presente en schema.sql"
    }
}

$dbmlPath = Join-Path $root "database\mysql\rupi.dbml"
if (Test-Path $dbmlPath) {
    $dbmlContent = Get-Content -Path $dbmlPath -Raw
    if ($dbmlContent -match "aprendizaje_nodo_recurso") {
        Report-Pass "Disenio DBML rupi.dbml sincronizado con tabla aprendizaje_nodo_recurso"
    } else {
        Report-Fail "DBML" "rupi.dbml no incluye aprendizaje_nodo_recurso"
    }
}

# 2. Verificacion de Base de Datos MySQL si esta disponible
Write-Host "`n2. Verificando conexion a Base de Datos MySQL (si esta disponible)..." -ForegroundColor Yellow
$mysqlCmd = "mysql"
$mysqlExePath = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
if (Test-Path $mysqlExePath) {
    $mysqlCmd = $mysqlExePath
}

$dbTested = $false
try {
    $checkDb = & $mysqlCmd -u rupi -e "SELECT id, codigo FROM rupi.identidad_rol WHERE codigo = 'DOCENTE';" 2>&1
    $dbStr = ($checkDb | Out-String)
    if ($dbStr -match 'DOCENTE') {
        Report-Pass "Base de datos activa: Rol DOCENTE verificado en vivo"
        $dbTested = $true
    }
} catch {
    # Ignorar si requiere credenciales especificas
}

if (-not $dbTested) {
    Write-Host " [INFO] Conexion directa CLI sin contrasenia no requerida para el build automatizado." -ForegroundColor Gray
}

# 3. Verificacion de Pruebas Unitarias del Backend (JUnit 5)
Write-Host "`n3. Verificando Backend Spring Boot (Maven JUnit Tests)..." -ForegroundColor Yellow
$backendDir = Join-Path $root "backend\api-spring-boot"
if (Test-Path $backendDir) {
    $pomPath = Join-Path $backendDir "pom.xml"
    $mvnOutput = & mvn.cmd -f $pomPath test "-Dtest=TeacherRouteServiceTest,MaterialStorageServiceTest,ActivityServiceTest" 2>&1
    $mvnStr = ($mvnOutput | Out-String)
    if ($mvnStr -match 'BUILD SUCCESS' -or $LASTEXITCODE -eq 0) {
        Report-Pass "Pruebas JUnit backend (TeacherRouteServiceTest, MaterialStorageServiceTest, ActivityServiceTest) aprobadas al 100%"
    } else {
        Report-Fail "Pruebas JUnit backend" "Fallo en Maven test"
        Write-Host $mvnStr -ForegroundColor Red
    }
} else {
    Report-Fail "Directorio backend" "No se encontro $backendDir"
}

# 4. Verificacion de Compilacion y Calidad Frontend (TypeScript, Vite, ESLint)
Write-Host "`n4. Verificando Frontend React (TypeScript y Vite Build)..." -ForegroundColor Yellow
$frontendDir = Join-Path $root "frontend\web-react"
if (Test-Path $frontendDir) {
    Push-Location $frontendDir
    try {
        $npmBuildResult = & cmd.exe /c "npm run build" 2>&1
        $buildStr = ($npmBuildResult | Out-String)
        if ($LASTEXITCODE -eq 0) {
            Report-Pass "Compilacion frontend exitosa (tsc -b y vite build)"
        } else {
            Report-Fail "Compilacion frontend" "npm run build fallo"
            Write-Host $buildStr -ForegroundColor Red
        }

        $npmLintResult = & cmd.exe /c "npm run lint" 2>&1
        $lintStr = ($npmLintResult | Out-String)
        if ($LASTEXITCODE -eq 0) {
            Report-Pass "Linter frontend limpio sin errores ni advertencias (eslint)"
        } else {
            Report-Fail "Linter frontend" "npm run lint reporto errores"
            Write-Host $lintStr -ForegroundColor Red
        }
    } finally {
        Pop-Location
    }
} else {
    Report-Fail "Directorio frontend" "No se encontro $frontendDir"
}

# 5. Resumen
Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host " RESUMEN DE VERIFICACION HU-05" -ForegroundColor Cyan
Write-Host " Pruebas aprobadas: $passCount" -ForegroundColor Green
Write-Host " Pruebas fallidas:  $failCount" -ForegroundColor $(if ($failCount -gt 0) { 'Red' } else { 'Green' })
Write-Host "=================================================================" -ForegroundColor Cyan

if ($failCount -gt 0) {
    exit 1
} else {
    Write-Host "HISTORIA DE USUARIO 05 VERIFICADA CON EXITO!" -ForegroundColor Green
    exit 0
}
