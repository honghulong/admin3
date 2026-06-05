<#
.SYNOPSIS
    Admin3 full build script (frontend + backend packaging)
.DESCRIPTION
    Steps: 1) vite build frontend, 2) stop Java processes (avoid jar lock),
    3) mvn clean package backend (produces fat JAR with frontend resources).
.EXAMPLE
    .\build.ps1                    # full build
    .\build.ps1 -SkipFrontend      # skip frontend build if dist exists
    .\build.ps1 -SkipClean         # skip mvn clean (incremental compile)
#>

param(
    [switch]$SkipFrontend,
    [switch]$SkipClean
)

$ErrorActionPreference = "Stop"
$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Admin3 Build Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build frontend
if (-not $SkipFrontend) {
    Write-Host "[1/3] Building frontend (vite build)..." -ForegroundColor Yellow
    $frontendDir = Join-Path $rootDir "admin3-ui"

    Push-Location $frontendDir
    try {
        $distDir = Join-Path $frontendDir "dist"
        if (Test-Path $distDir) {
            Remove-Item -Recurse -Force $distDir
            Write-Host "  cleaned old dist directory" -ForegroundColor Gray
        }

        $viteBin = Join-Path $frontendDir "node_modules\vite\bin\vite.js"
        if (-not (Test-Path $viteBin)) {
            Write-Host "  [WARN] vite not found, running yarn install..." -ForegroundColor Yellow
            & yarn install
            if ($LASTEXITCODE -ne 0) { throw "yarn install failed" }
        }

        & node $viteBin build
        if ($LASTEXITCODE -ne 0) { throw "vite build failed" }

        if (-not (Test-Path $distDir)) {
            throw "vite build done but dist directory not found"
        }
        Write-Host "  frontend build complete: $distDir" -ForegroundColor Green
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Host "[1/3] Skipping frontend build" -ForegroundColor Gray
}

# Step 2: Stop Java processes that may lock JAR files
Write-Host "[2/3] Checking Java processes..." -ForegroundColor Yellow
$javaProcs = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcs) {
    $count = ($javaProcs | Measure-Object).Count
    Write-Host "  found $count Java process(es), stopping..." -ForegroundColor Yellow
    $javaProcs | Stop-Process -Force
    Start-Sleep -Seconds 1
    Write-Host "  Java processes stopped" -ForegroundColor Green
}
else {
    Write-Host "  no running Java processes" -ForegroundColor Gray
}

# Step 3: Maven package backend
Write-Host "[3/3] Maven packaging backend..." -ForegroundColor Yellow
$mvnw = Join-Path $rootDir "mvnw.cmd"

$mvnArgs = @()
if ($SkipClean) {
    $mvnArgs += "package"
    Write-Host "  mode: incremental (skip clean)" -ForegroundColor Gray
}
else {
    $mvnArgs += "clean"
    $mvnArgs += "package"
    Write-Host "  mode: full (clean)" -ForegroundColor Gray
}
$mvnArgs += "-pl"
$mvnArgs += "admin3-server"
$mvnArgs += "-am"
$mvnArgs += "-DskipTests"

Push-Location $rootDir
try {
    & $mvnw $mvnArgs
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
}
finally {
    Pop-Location
}

# Verify result
$jarFile = Join-Path $rootDir "admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar"
if (Test-Path $jarFile) {
    $size = (Get-Item $jarFile).Length
    $sizeMB = [math]::Round($size / 1MB, 1)
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  BUILD SUCCESS!" -ForegroundColor Green
    Write-Host "  JAR: $jarFile" -ForegroundColor Green
    Write-Host "  Size: ${sizeMB}MB" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan

    if ($size -gt 10MB) {
        Write-Host "  [OK] Frontend resources included in JAR" -ForegroundColor Green
    }
    else {
        Write-Host "  [WARN] JAR is small, frontend resources may be missing" -ForegroundColor Yellow
    }
}
else {
    Write-Host "[ERROR] JAR file not found: $jarFile" -ForegroundColor Red
    exit 1
}
