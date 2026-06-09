# BC Sports Admin Safe Startup Script
# Usage: .\start-safe.ps1

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  BC Sports Admin - Safe Startup" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Project directory
$projectDir = "D:\B.C.Sports\actions-runner\_work\BC_Sports\BC_Sports\project"

# 1. Stop old Java processes
Write-Host "[1/2] Stopping old Java processes..." -ForegroundColor Yellow
taskkill /F /IM java.exe 2>$null
Start-Sleep -Seconds 3
Write-Host "  Done" -ForegroundColor Green

# 2. Switch to project directory and start service
Write-Host ""
Write-Host "[2/2] Starting BC Sports Admin..." -ForegroundColor Yellow
Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "  Starting service:" -ForegroundColor Green
Write-Host "  - Initial memory: 8GB" -ForegroundColor Green
Write-Host "  - Max memory: 16GB" -ForegroundColor Green
Write-Host "  URL: http://192.168.5.180:8080" -ForegroundColor Green
Write-Host "  Press Ctrl+C to stop" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""

# Start Java using cmd (more reliable)
Set-Location $projectDir
cmd /c "java -Djava.security.properties=./tls-override.security -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.region=CN -Xms8g -Xmx16g -jar target/bc-sports-admin-1.0.0.jar --spring.profiles.active=prod"
