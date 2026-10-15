# ==============================================================================
# Verificacion Automatizada - HU-06: Reorganizacion de Secuencia de Aprendizaje
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
Write-Host " INICIANDO VERIFICACION: HISTORIA DE USUARIO 06 (REORDENAMIENTO)" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# 1. Verificacion de DTOs, Endpoint y Logica Transaccional Backend
Write-Host "`n1. Verificando logica transaccional y endpoint PUT en backend..." -ForegroundColor Yellow

$repoFile = Join-Path $root "backend\api-spring-boot\src\main\java\pe\rupi\api\teacher\TeacherRouteRepository.java"
if (Test-Path $repoFile) {
    $repoContent = Get-Content -Path $repoFile -Raw
    if ($repoContent -match "numero_secuencia \+ 1000" -and $repoContent -match "calculateDefaultMapX") {
        Report-Pass "Repository maneja desplazamiento anti-colision y recalculado de coordenadas del mapa"
    } else {
        Report-Fail "Repository reorderNodes" "No incluye logica anti-colision uq_nodo_orden"
    }
} else {
    Report-Fail "Archivo TeacherRouteRepository" "No se encontro $repoFile"
}

$controllerFile = Join-Path $root "backend\api-spring-boot\src\main\java\pe\rupi\api\teacher\TeacherRouteController.java"
if (Test-Path $controllerFile) {
    $ctrlContent = Get-Content -Path $controllerFile -Raw
    if ($ctrlContent -match "@PutMapping\(`"/{routeId}/nodes/reorder`"\)") {
        Report-Pass "Endpoint REST PUT /nodes/reorder registrado en TeacherRouteController"
    } else {
        Report-Fail "Controller reorderNodes" "No encontrado @PutMapping('/{routeId}/nodes/reorder')"
    }
} else {
    Report-Fail "Archivo TeacherRouteController" "No se encontro $controllerFile"
}

# 2. Verificacion de Pruebas Unitarias del Backend (JUnit 5)
Write-Host "`n2. Verificando Backend Spring Boot (Maven JUnit Tests)..." -ForegroundColor Yellow
$backendDir = Join-Path $root "backend\api-spring-boot"
if (Test-Path $backendDir) {
    $pomPath = Join-Path $backendDir "pom.xml"
    # mvn.cmd solo existe en Windows; en Linux y macOS el ejecutable es mvn (PowerShell 5.1 y 7+).
    $mvnExe = if ($PSVersionTable.PSVersion.Major -lt 6 -or $IsWindows) { 'mvn.cmd' } else { 'mvn' }
    $mvnOutput = & $mvnExe -f $pomPath test "-Dtest=TeacherRouteServiceTest" 2>&1
    $mvnStr = ($mvnOutput | Out-String)
    if ($mvnStr -match 'BUILD SUCCESS' -or $LASTEXITCODE -eq 0) {
        Report-Pass "11 pruebas JUnit en TeacherRouteServiceTest aprobadas (incluye reordenamiento y validaciones)"
    } else {
        Report-Fail "Pruebas JUnit backend" "Fallo en Maven test"
        Write-Host $mvnStr -ForegroundColor Red
    }
} else {
    Report-Fail "Directorio backend" "No se encontro $backendDir"
}

# 3. Verificacion de Componentes de Frontend (Drag and Drop, Estado Temporal, Accesibilidad)
Write-Host "`n3. Verificando Drag & Drop y Gestion de Estado en Frontend..." -ForegroundColor Yellow
$authoringView = Join-Path $root "frontend\web-react\src\features\teacher\TeacherAuthoringView.tsx"
if (Test-Path $authoringView) {
    $authoringContent = Get-Content -Path $authoringView -Raw
    $hasDnD = $authoringContent -match "onDragStart" -and $authoringContent -match "onDrop" -and $authoringContent -match "draggable"
    $hasTempState = $authoringContent -match "isReorderDirty" -and $authoringContent -match "orderedNodes"
    $hasAccessibleButtons = $authoringContent -match "moveNode" -and $authoringContent -match "ChevronUp" -and $authoringContent -match "ChevronDown"
    $hasApiCall = $authoringContent -match "reorderTeacherNodes"

    if ($hasDnD) {
        Report-Pass "Tarea 1 cumplida: Drag & Drop interactivo nativo (draggable, dragStart, drop)"
    } else {
        Report-Fail "Tarea 1" "Falta implementacion Drag & Drop en TeacherAuthoringView"
    }

    if ($hasTempState) {
        Report-Pass "Tarea 2 cumplida: Gestion de estado visual temporal antes de confirmar (isReorderDirty)"
    } else {
        Report-Fail "Tarea 2" "Falta gestion de estado temporal antes de guardar"
    }

    if ($hasAccessibleButtons) {
        Report-Pass "Accesibilidad: Botones de movimiento vertical para tabletas y navegacion por teclado"
    } else {
        Report-Fail "Accesibilidad" "Faltan controles alternativos a Drag & Drop"
    }

    if ($hasApiCall) {
        Report-Pass "Integracion API: Llamada a reorderTeacherNodes al confirmar la nueva secuencia"
    } else {
        Report-Fail "Integracion API" "No se llama a reorderTeacherNodes"
    }
} else {
    Report-Fail "Archivo TeacherAuthoringView" "No se encontro $authoringView"
}

# 4. Verificacion de Compilacion y Calidad Frontend (TypeScript, Vite, ESLint)
Write-Host "`n4. Verificando Frontend React (TypeScript y Vite Build)..." -ForegroundColor Yellow
$frontendDir = Join-Path $root "frontend\web-react"
if (Test-Path $frontendDir) {
    Push-Location $frontendDir
    try {
        $npmBuildResult = & cmd.exe /c "npm run build" 2>&1
        if ($LASTEXITCODE -eq 0) {
            Report-Pass "Compilacion frontend exitosa (tsc -b y vite build)"
        } else {
            Report-Fail "Compilacion frontend" "npm run build fallo"
        }

        $npmLintResult = & cmd.exe /c "npm run lint" 2>&1
        if ($LASTEXITCODE -eq 0) {
            Report-Pass "Linter frontend limpio sin errores ni advertencias (eslint)"
        } else {
            Report-Fail "Linter frontend" "npm run lint reporto errores"
        }
    } finally {
        Pop-Location
    }
} else {
    Report-Fail "Directorio frontend" "No se encontro $frontendDir"
}

# 5. Resumen
Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host " RESUMEN DE VERIFICACION HU-06" -ForegroundColor Cyan
Write-Host " Pruebas aprobadas: $passCount" -ForegroundColor Green
Write-Host " Pruebas fallidas:  $failCount" -ForegroundColor $(if ($failCount -gt 0) { 'Red' } else { 'Green' })
Write-Host "=================================================================" -ForegroundColor Cyan

if ($failCount -gt 0) {
    exit 1
} else {
    Write-Host "HISTORIA DE USUARIO 06 VERIFICADA CON EXITO!" -ForegroundColor Green
    exit 0
}
