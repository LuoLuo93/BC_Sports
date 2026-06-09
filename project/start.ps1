# BC Sports Admin 启动脚本
# 使用方法: 右键 -> 使用 PowerShell 运行，或者在 PowerShell 中执行: .\start.ps1

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  BC体育后台管理系统 - 启动脚本" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# 1. 停止旧的 Java 进程
Write-Host "[1/3] 停止旧的 Java 进程..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Stop-Process -Name "java" -Force
    Start-Sleep -Seconds 2
    Write-Host "  已停止 $($javaProcesses.Count) 个 Java 进程" -ForegroundColor Green
} else {
    Write-Host "  没有正在运行的 Java 进程" -ForegroundColor Gray
}

# 2. 切换到项目目录
Write-Host ""
Write-Host "[2/3] 切换到项目目录..." -ForegroundColor Yellow
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath
Write-Host "  当前目录: $scriptPath" -ForegroundColor Gray

# 3. 启动服务
Write-Host ""
Write-Host "[3/3] 启动 BC Sports Admin..." -ForegroundColor Yellow
Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "  服务启动中，请等待..." -ForegroundColor Green
Write-Host "  访问地址: http://192.168.5.180:8080" -ForegroundColor Green
Write-Host "  API文档: http://192.168.5.180:8080/doc.html" -ForegroundColor Green
Write-Host "  按 Ctrl+C 停止服务" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""

# 启动 Java 应用（前台运行，方便查看日志）
java "-Djava.security.properties=./tls-override.security" `
     -Dfile.encoding=UTF-8 `
     -Dsun.jnu.encoding=UTF-8 `
     -Duser.language=zh `
     -Duser.region=CN `
     -jar target/bc-sports-admin-1.0.0.jar `
     --spring.profiles.active=prod
