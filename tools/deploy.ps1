[CmdletBinding()]
param(
    [string]$ProjectRoot = 'D:\hongmengProjects\admin3',
    [switch]$Deploy
)
$ErrorActionPreference = 'Stop'
$key = Join-Path $env:USERPROFILE '.ssh\doc_gpu_server_ed25519'
$target = 'administrator@120.234.136.122'
$sshOptions = @('-i', $key, '-p', '22258', '-o', 'BatchMode=yes', '-o', 'ConnectTimeout=15')
$jar = Join-Path $ProjectRoot 'admin3-server\target\admin3-server-0.0.1-SNAPSHOT.jar'
if (!(Test-Path -LiteralPath $key)) { throw 'SSH private key not found.' }
if (!(Test-Path -LiteralPath $jar)) { throw "Build the package first: $jar" }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $manifest = $archive.GetEntry('META-INF/MANIFEST.MF')
    if (!$manifest) { throw 'Not a JAR with a manifest.' }
    $reader = New-Object IO.StreamReader($manifest.Open())
    try { $manifestText = $reader.ReadToEnd() } finally { $reader.Dispose() }
    if ($manifestText -notmatch 'Main-Class: org.springframework.boot.loader' -or
        $manifestText -notmatch 'Start-Class: tech.wetech.admin3.Admin3ServerApplication') {
        throw 'Not the executable admin3 Spring Boot package.'
    }
    $frontendEntry = $archive.GetEntry('BOOT-INF/lib/admin3-ui-0.0.1-SNAPSHOT.jar')
    if (!$frontendEntry) { throw 'Bundled frontend missing.' }
    $buffer = New-Object IO.MemoryStream
    $entryStream = $frontendEntry.Open()
    try {
        $entryStream.CopyTo($buffer)
        $buffer.Position = 0
        $frontend = New-Object IO.Compression.ZipArchive($buffer, [IO.Compression.ZipArchiveMode]::Read, $true)
        try {
            if (!$frontend.GetEntry('META-INF/resources/webjars/admin3-ui/index.html')) {
                throw 'Bundled frontend index missing.'
            }
        } finally { $frontend.Dispose() }
    } finally { $entryStream.Dispose(); $buffer.Dispose() }
} finally { $archive.Dispose() }
$package = Get-Item -LiteralPath $jar
$sourcePaths = @('admin3-server\src', 'admin3-ui\src')
$newerSources = @(foreach ($relative in $sourcePaths) {
    Get-ChildItem -LiteralPath (Join-Path $ProjectRoot $relative) -File -Recurse |
        Where-Object { $_.LastWriteTimeUtc -gt $package.LastWriteTimeUtc }
})
foreach ($relative in @('pom.xml', 'admin3-server\pom.xml', 'admin3-ui\pom.xml', 'admin3-ui\package.json', 'admin3-ui\yarn.lock')) {
    $source = Get-Item -LiteralPath (Join-Path $ProjectRoot $relative) -ErrorAction SilentlyContinue
    if ($source -and $source.LastWriteTimeUtc -gt $package.LastWriteTimeUtc) { $newerSources += $source }
}
if ($newerSources) { throw 'Sources/configuration are newer than the package; rebuild before deploying.' }
$hash = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
Write-Output "Package: $jar"
Write-Output "Bytes: $($package.Length); SHA256: $hash"

function Invoke-Remote([string]$Code) {
    $wrapped = '$ErrorActionPreference = ''Stop''; $ProgressPreference = ''SilentlyContinue''; try {' +
        $Code + '} catch { Write-Error $_; exit 1 }'
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($wrapped))
    $remoteCommand = 'powershell.exe -NoProfile -NonInteractive -Command "Invoke-Expression ([Text.Encoding]::Unicode.GetString([Convert]::FromBase64String([Console]::In.ReadToEnd().Trim())))"'
    $encoded | & ssh @sshOptions $target $remoteCommand
    if ($LASTEXITCODE -ne 0) { throw "Remote operation failed (SSH exit $LASTEXITCODE)." }
}

$preflight = @'
$dir = 'D:\services\admin3'
$jar = Join-Path $dir 'admin3-server-0.0.1-SNAPSHOT.jar'
$start = Join-Path $dir 'start_admin3.ps1'
foreach ($path in @($jar, $start, (Join-Path $dir 'secrets.env'))) {
    if (!(Test-Path -LiteralPath $path -PathType Leaf)) { throw "Remote prerequisite missing: $path" }
}
$task = Get-ScheduledTask -TaskName 'Admin3' -TaskPath '\'
if (!($task.Actions | Where-Object { $_.Arguments -like '*D:\services\admin3\start_admin3.ps1*' })) {
    throw 'Admin3 task does not target the expected startup script.'
}
$processes = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
    Where-Object { $_.CommandLine -and $_.CommandLine.IndexOf($jar, [StringComparison]::OrdinalIgnoreCase) -ge 0 })
Write-Output "Remote target: $jar; task=$($task.State); process count=$($processes.Count); port=19099"
'@
Invoke-Remote $preflight
if (!$Deploy) {
    Write-Output 'Preflight passed. No remote files or services changed. Use -Deploy to deploy.'
    return
}

$uploadName = 'admin3-upload-' + [Guid]::NewGuid().ToString('N') + '.jar'
$remoteUpload = 'D:/services/admin3/' + $uploadName
& scp -O -i $key -P 22258 -o BatchMode=yes -o ConnectTimeout=15 $jar "${target}:$remoteUpload"
if ($LASTEXITCODE -ne 0) { throw 'Upload failed; target service was not stopped.' }

$deployment = @'
$dir = 'D:\services\admin3'
$jar = Join-Path $dir 'admin3-server-0.0.1-SNAPSHOT.jar'
$incoming = Join-Path $dir '__UPLOAD__'
$expectedHash = '__HASH__'
$lock = Join-Path $dir 'deploy.lock'
$lockStream = [IO.File]::Open($lock, [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
$backup = $null
$stopped = $false
$task = Get-ScheduledTask -TaskName 'Admin3' -TaskPath '\'
$wasEnabled = $task.Settings.Enabled
function Stop-Target {
    Stop-ScheduledTask -TaskName 'Admin3' -TaskPath '\'
    Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
        Where-Object { $_.CommandLine -and $_.CommandLine.IndexOf($jar, [StringComparison]::OrdinalIgnoreCase) -ge 0 } |
        ForEach-Object {
            Stop-Process -Id $_.ProcessId -Force
            Wait-Process -Id $_.ProcessId -Timeout 15 -ErrorAction SilentlyContinue
        }
}
function Start-And-Verify {
    Enable-ScheduledTask -TaskName 'Admin3' -TaskPath '\' | Out-Null
    Start-ScheduledTask -TaskName 'Admin3' -TaskPath '\'
    $deadline = (Get-Date).AddSeconds(90)
    do {
        Start-Sleep -Seconds 3
        $process = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
            Where-Object { $_.CommandLine -and $_.CommandLine.IndexOf($jar, [StringComparison]::OrdinalIgnoreCase) -ge 0 }
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:19099/admin3/' -TimeoutSec 5
            if ($process -and $response.StatusCode -eq 200) { return }
        } catch { }
    } while ((Get-Date) -lt $deadline)
    throw 'Admin3 did not become healthy within 90 seconds.'
}
try {
    if ((Get-FileHash -LiteralPath $incoming -Algorithm SHA256).Hash -ne $expectedHash) {
        throw 'Uploaded package SHA256 mismatch; service was not stopped.'
    }
    $backupDir = Join-Path $dir 'backups'
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
    $backup = Join-Path $backupDir ('admin3-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N') + '.jar')
    Copy-Item -LiteralPath $jar -Destination $backup
    Disable-ScheduledTask -TaskName 'Admin3' -TaskPath '\' | Out-Null
    $stopped = $true
    Stop-Target
    Copy-Item -LiteralPath $incoming -Destination $jar -Force
    Start-And-Verify
    Write-Output "DEPLOYED SHA256=$expectedHash; BACKUP=$backup; HTTP=200"
} catch {
    $failure = $_
    if ($stopped -and $backup) {
        try {
            Stop-Target
            Copy-Item -LiteralPath $backup -Destination $jar -Force
            Start-And-Verify
            Write-Output "ROLLED BACK to $backup; HTTP=200"
        } catch { throw "Deployment failed and rollback failed. Backup=$backup; rollback error=$($_.Exception.Message)" }
    }
    throw $failure
} finally {
    if (!$wasEnabled) { Disable-ScheduledTask -TaskName 'Admin3' -TaskPath '\' | Out-Null }
    elseif ($stopped) { Enable-ScheduledTask -TaskName 'Admin3' -TaskPath '\' | Out-Null }
    $lockStream.Dispose()
    Remove-Item -LiteralPath $incoming -Force -ErrorAction SilentlyContinue
}
'@
Invoke-Remote ($deployment.Replace('__UPLOAD__', $uploadName).Replace('__HASH__', $hash))
Write-Output 'Application URL: http://120.234.136.122:19099/admin3/'
