param(
    [int]$Port = 9099,
    [string]$DbUrl = "jdbc:mysql://127.0.0.1:3306/admin3?characterEncoding=utf8",
    [string]$DbUsername = "root",
    [string]$DbPassword = "123456"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$JarFile = Join-Path $ProjectRoot "admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar"
$LogDir = Join-Path $ProjectRoot "logs"
$LogFile = Join-Path $LogDir "admin3-server.log"
$ErrLogFile = Join-Path $LogDir "admin3-server.err.log"
$PidFile = Join-Path $LogDir "admin3-server.pid"
$JavaCandidates = @()
if ($env:JAVA_HOME) {
    $JavaCandidates += (Join-Path $env:JAVA_HOME "bin\java.exe")
}
$PathJava = Get-Command java -ErrorAction SilentlyContinue
if ($PathJava) {
    $JavaCandidates += $PathJava.Source
}
$JavaCandidates += @(
    "D:\java\jdk21.0.11-win_x64\bin\java.exe",
    "C:\JAVA\jdk21.0.11-win_x64\bin\java.exe"
)
$JavaExe = $JavaCandidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1

if (-not $JavaExe) {
    Write-Host "[ERROR] Cannot find java.exe. Please set JAVA_HOME or add Java to PATH."
    exit 1
}

if (-not (Test-Path $JarFile)) {
    Write-Host "[ERROR] Cannot find JAR file: $JarFile"
    Write-Host "Please run 'mvn clean package -DskipTests' first"
    exit 1
}

if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
}

if (Test-Path $PidFile) {
    $OldPid = (Get-Content $PidFile -Raw).Trim()
    $proc = $null
    if ($OldPid -match '^\d+$') {
        $proc = Get-Process -Id ([int]$OldPid) -ErrorAction SilentlyContinue
    }
    if ($proc -ne $null) {
        Write-Host "[INFO] Service already running (PID: $OldPid)"
        Write-Host "[INFO] Log file: $LogFile"
        Write-Host "[INFO] URL: http://localhost:$Port/admin3"
        exit 0
    }
    Remove-Item $PidFile -Force
}

Write-Host "[INFO] Starting: $JarFile"
Write-Host "[INFO] URL: http://localhost:$Port/admin3"
Write-Host "[INFO] Log file: $LogFile"
Write-Host ""

$JavaArgs = @(
    "-Dspring.datasource.url=$DbUrl",
    "-Dspring.datasource.username=$DbUsername",
    "-Dspring.datasource.password=$DbPassword",
    "-Dlogging.file.name=$LogFile",
    "-jar",
    $JarFile,
    "--server.port=$Port"
)

$process = Start-Process `
    -FilePath $JavaExe `
    -ArgumentList $JavaArgs `
    -WorkingDirectory $ProjectRoot `
    -WindowStyle Hidden `
    -PassThru

$process.Id | Set-Content -Path $PidFile -Encoding ascii

Start-Sleep -Seconds 2
$started = Get-Process -Id $process.Id -ErrorAction SilentlyContinue
if (-not $started) {
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    Write-Host "[ERROR] Service exited during startup. Check logs:"
    Write-Host "        $LogFile"
    Write-Host "        $ErrLogFile"
    exit 1
}

Write-Host "[INFO] Service started (PID: $($process.Id))"
Write-Host "[INFO] Stop with: .\stop.ps1"
