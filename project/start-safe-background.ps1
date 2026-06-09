# BC Sports Admin Safe Background Startup Script (Memory Limited)
# Usage: .\start-safe-background.ps1

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  BC Sports Admin - Safe Background" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Project directory
$projectDir = "D:\B.C.Sports\actions-runner\_work\BC_Sports\BC_Sports\project"

# 1. Stop old Java processes
Write-Host "[1/3] Stopping old Java processes..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Stop-Process -Name "java" -Force
    Start-Sleep -Seconds 3
    Write-Host "  Stopped Java processes" -ForegroundColor Green
} else {
    Write-Host "  No running Java processes found" -ForegroundColor Gray
}

# 2. Switch to project directory
Write-Host ""
Write-Host "[2/3] Switching to project directory..." -ForegroundColor Yellow
Set-Location $projectDir
Write-Host "  Current directory: $projectDir" -ForegroundColor Gray

# 3. Start service with memory limits in background
Write-Host ""
Write-Host "[3/3] Starting BC Sports Admin (Background, Memory Limited)..." -ForegroundColor Yellow

$javaArgs = @(
    "-Djava.security.properties=./tls-override.security",
    "-Dfile.encoding=UTF-8",
    "-Dsun.jnu.encoding=UTF-8",
    "-Duser.language=zh",
    "-Duser.region=CN",
    "-Xms256m",
    "-Xmx512m",
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=200",
    "-XX:+HeapDumpOnOutOfMemoryError",
    "-XX:HeapDumpPath=./logs/heapdump.hprof",
    "-jar",
    "target/bc-sports-admin-1.0.0.jar",
    "--spring.profiles.active=prod"
)

Start-Process -FilePath "java" -ArgumentList $javaArgs -WindowStyle Hidden

Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "  Service started in background!" -ForegroundColor Green
Write-Host "  Memory limit: 512MB" -ForegroundColor Green
Write-Host "  URL: http://192.168.5.180:8080" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""

# Wait for service to start
Write-Host "Waiting for service to start (20 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 20

# Check if service started
$javaProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcess) {
    $memUsage = [math]::Round($javaProcess.WorkingSet64 / 1MB, 2)
    Write-Host "Service started successfully!" -ForegroundColor Green
    Write-Host "  PID: $($javaProcess.Id)" -ForegroundColor Gray
    Write-Host "  Memory: $memUsage MB" -ForegroundColor Gray
} else {
    Write-Host "ERROR: Service failed to start!" -ForegroundColor Red
    Write-Host "Check logs: Get-Content logs\admin.log -Tail 50" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Cyan
Write-Host "  View logs: Get-Content logs\admin.log -Tail 50" -ForegroundColor Gray
Write-Host "  Stop service: taskkill /F /IM java.exe" -ForegroundColor Gray
Write-Host "  Check memory: Get-Process java | Select-Object Id, WorkingSet64" -ForegroundColor Gray
