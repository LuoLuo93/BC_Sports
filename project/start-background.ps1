# BC Sports Admin Background Startup Script
# Usage: Right-click -> Run with PowerShell, or execute in PowerShell: .\start-background.ps1

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  BC Sports Admin - Background Start" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# 1. Stop old Java processes
Write-Host "[1/3] Stopping old Java processes..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Stop-Process -Name "java" -Force
    Start-Sleep -Seconds 2
    Write-Host "  Stopped $($javaProcesses.Count) Java process(es)" -ForegroundColor Green
} else {
    Write-Host "  No running Java processes found" -ForegroundColor Gray
}

# 2. Switch to project directory
Write-Host ""
Write-Host "[2/3] Switching to project directory..." -ForegroundColor Yellow
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath
Write-Host "  Current directory: $scriptPath" -ForegroundColor Gray

# 3. Start service in background
Write-Host ""
Write-Host "[3/3] Starting BC Sports Admin in background..." -ForegroundColor Yellow

$javaArgs = @(
    "-Djava.security.properties=./tls-override.security",
    "-Dfile.encoding=UTF-8",
    "-Dsun.jnu.encoding=UTF-8",
    "-Duser.language=zh",
    "-Duser.region=CN",
    "-jar",
    "target/bc-sports-admin-1.0.0.jar",
    "--spring.profiles.active=prod"
)

Start-Process -FilePath "java" -ArgumentList $javaArgs -WindowStyle Hidden

Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "  Service started in background!" -ForegroundColor Green
Write-Host "  URL: http://192.168.5.180:8080" -ForegroundColor Green
Write-Host "  API Docs: http://192.168.5.180:8080/doc.html" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""

# Wait for service to start
Write-Host "Waiting for service to start (15 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Check if service started successfully
$javaProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcess) {
    Write-Host "Service started successfully! Java PID: $($javaProcess.Id)" -ForegroundColor Green
} else {
    Write-Host "Warning: Java process not detected, startup may have failed" -ForegroundColor Red
    Write-Host "Check logs: Get-Content logs\admin.log -Tail 50" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Cyan
Write-Host "  View logs: Get-Content logs\admin.log -Tail 50" -ForegroundColor Gray
Write-Host "  Stop service: taskkill /F /IM java.exe" -ForegroundColor Gray
Write-Host "  View processes: tasklist | findstr java" -ForegroundColor Gray
