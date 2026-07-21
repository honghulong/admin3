$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $ProjectRoot "logs\admin3-server.pid"

if (Test-Path $PidFile) {
    $ServicePid = Get-Content $PidFile -Raw | ForEach-Object { $_.Trim() }
    if ($ServicePid -notmatch '^\d+$') {
        Write-Host "[WARN] Invalid PID file content. Removing stale PID file."
        Remove-Item $PidFile -Force
        exit 0
    }

    Write-Host "[INFO] Stopping service (PID: $ServicePid)..."
    $proc = Get-Process -Id ([int]$ServicePid) -ErrorAction SilentlyContinue
    if ($proc) {
        Stop-Process -Id ([int]$ServicePid) -Force
        Write-Host "[INFO] Service stopped."
    } else {
        Write-Host "[WARN] Process $ServicePid not found, may have already exited."
    }
    Remove-Item $PidFile -Force
} else {
    Write-Host "[INFO] No PID file found. Nothing to stop."
}
