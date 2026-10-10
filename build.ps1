<#
.SYNOPSIS
    Clean, compile and package frontend + backend into one executable JAR.
.EXAMPLE
    .\build.bat                  # clean package, skip tests
    .\build.bat -Restart         # clean package, then start service
    .\build.bat -RunTests        # clean package with tests
    .\build.bat -SkipClean       # package without clean
#>
param(
    [switch]$SkipClean,
    [switch]$RunTests,
    [switch]$Restart
)

$ErrorActionPreference = "Stop"
$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarFile = Join-Path $rootDir "admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar"
$mvnw = Join-Path $rootDir "mvnw.cmd"

try {
    # Match this project's JAR to avoid stopping unrelated Java applications.
    Write-Host "[1/3] Checking running admin3 service..."
    $javaProcs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
        Where-Object { $_.CommandLine -and $_.CommandLine.IndexOf($jarFile, [StringComparison]::OrdinalIgnoreCase) -ge 0 }
    foreach ($proc in $javaProcs) {
        Write-Host "Stopping admin3 (PID: $($proc.ProcessId))"
        Stop-Process -Id $proc.ProcessId -Force
        Wait-Process -Id $proc.ProcessId -Timeout 15 -ErrorAction SilentlyContinue
    }

    $mvnArgs = @()
    if (-not $SkipClean) { $mvnArgs += "clean" }
    $mvnArgs += "package"
    if (-not $RunTests) { $mvnArgs += "-DskipTests" }

    Write-Host "[2/3] Cleaning, compiling and packaging frontend + backend..."
    Write-Host "Command: .\mvnw.cmd $($mvnArgs -join ' ')"
    Push-Location $rootDir
    try {
        & $mvnw $mvnArgs
        if ($LASTEXITCODE -ne 0) { throw "Maven build failed (exit code $LASTEXITCODE)." }
    }
    finally { Pop-Location }

    Write-Host "[3/3] Verifying bundled frontend..."
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($jarFile)
    try {
        $uiEntry = $archive.GetEntry("BOOT-INF/lib/admin3-ui-0.0.1-SNAPSHOT.jar")
        if (-not $uiEntry) { throw "Frontend JAR is missing from backend JAR." }
        $buffer = New-Object IO.MemoryStream
        $entryStream = $uiEntry.Open()
        try {
            $entryStream.CopyTo($buffer)
            $buffer.Position = 0
            $frontend = New-Object IO.Compression.ZipArchive($buffer, [IO.Compression.ZipArchiveMode]::Read, $true)
            try {
                if (-not $frontend.GetEntry("META-INF/resources/webjars/admin3-ui/index.html")) {
                    throw "Frontend index.html is missing from bundled frontend JAR."
                }
            }
            finally { $frontend.Dispose() }
        }
        finally { $entryStream.Dispose(); $buffer.Dispose() }
    }
    finally { $archive.Dispose() }

    Write-Host "BUILD SUCCESS: $jarFile" -ForegroundColor Green
    Write-Host "Verified: bundled frontend JAR and index.html."
    if ($Restart) {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $rootDir "start.ps1")
        if ($LASTEXITCODE -ne 0) { throw "Service start failed." }
    }
    else { Write-Host "Start service: .\start.bat" }
}
catch {
    Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
