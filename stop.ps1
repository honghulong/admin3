$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $ProjectRoot "logs\admin3-server.pid"

if (Test-Path $PidFile) {
    $PID = Get-Content $PidFile -Raw | ForEach-Object { $_.Trim() }
    Write-Host "[INFO] Stopping service (PID: $PID)..."
    $proc = Get-Process -Id $PID -ErrorAction SilentlyContinue
    if ($proc) {
        Stop-Process -Id $PID -Force
        Write-Host "[INFO] Service stopped."
    } else {
        Write-Host "[WARN] Process $PID not found, may have already exited."
    }
    Remove-Item $PidFile -Force
} else {
    Write-Host "[INFO] No PID file found. Trying to kill all java.exe..."
    Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
    Write-Host "[INFO] Done."
}
