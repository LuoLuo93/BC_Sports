# BC Sports Admin Startup Script
# Usage: Right-click -> Run with PowerShell, or execute in PowerShell: .\start.ps1

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  BC Sports Admin - Startup Script" -ForegroundColor Cyan
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

# 3. Start service
Write-Host ""
Write-Host "[3/3] Starting BC Sports Admin..." -ForegroundColor Yellow
Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "  Starting service, please wait..." -ForegroundColor Green
Write-Host "  URL: http://192.168.5.180:8080" -ForegroundColor Green
Write-Host "  API Docs: http://192.168.5.180:8080/doc.html" -ForegroundColor Green
Write-Host "  Press Ctrl+C to stop" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""

# Start Java application (foreground mode for viewing logs)
java "-Djava.security.properties=./tls-override.security" `
     -Dfile.encoding=UTF-8 `
     -Dsun.jnu.encoding=UTF-8 `
     -Duser.language=zh `
     -Duser.region=CN `
     -jar target/bc-sports-admin-1.0.0.jar `
     --spring.profiles.active=prod
