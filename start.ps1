param(
    [int]$Port = 9099
)

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$JarFile = Join-Path $ProjectRoot "admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar"
$LogDir = Join-Path $ProjectRoot "logs"
$LogFile = Join-Path $LogDir "admin3-server.log"
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
    $OldPid = Get-Content $PidFile
    $proc = Get-Process -Id $OldPid -ErrorAction SilentlyContinue
    if ($proc -ne $null) {
        Write-Host "[INFO] Service already running (PID: $OldPid)"
        Write-Host "[INFO] Log file: $LogFile"
        Write-Host "[INFO] URL: http://localhost:$Port/admin3"
        exit 0
    }
}

Write-Host "[INFO] Starting: $JarFile"
Write-Host "[INFO] URL: http://localhost:$Port/admin3"
Write-Host "[INFO] Log file: $LogFile"
Write-Host "[INFO] Press Ctrl+C to stop"
Write-Host ""

$JavaArgs = @(
    "-Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/admin3?characterEncoding=utf8",
    "-Dspring.datasource.username=root",
    "-Dspring.datasource.password=123456",
    "-jar",
    $JarFile,
    "--server.port=$Port"
)

& $JavaExe @JavaArgs 2>&1 | ForEach-Object { $_; $_ | Out-File -FilePath $LogFile -Encoding utf8 -Append }

Write-Host ""
Write-Host "[INFO] Service stopped"

if (Test-Path $PidFile) {
    Remove-Item $PidFile -Force
}
