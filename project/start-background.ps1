# BC Sports Admin 后台启动脚本
# 使用方法: 右键 -> 使用 PowerShell 运行，或者在 PowerShell 中执行: .\start-background.ps1

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  BC体育后台管理系统 - 后台启动" -ForegroundColor Cyan
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

# 3. 后台启动服务
Write-Host ""
Write-Host "[3/3] 后台启动 BC Sports Admin..." -ForegroundColor Yellow

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
Write-Host "  服务已在后台启动!" -ForegroundColor Green
Write-Host "  访问地址: http://192.168.5.180:8080" -ForegroundColor Green
Write-Host "  API文档: http://192.168.5.180:8080/doc.html" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""

# 等待服务启动
Write-Host "等待服务启动 (15秒)..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# 检查服务是否启动成功
$javaProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcess) {
    Write-Host "服务启动成功! Java PID: $($javaProcess.Id)" -ForegroundColor Green
} else {
    Write-Host "警告: 未检测到 Java 进程，可能启动失败" -ForegroundColor Red
    Write-Host "请查看日志: Get-Content logs\admin.log -Tail 50" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "常用命令:" -ForegroundColor Cyan
Write-Host "  查看日志: Get-Content logs\admin.log -Tail 50" -ForegroundColor Gray
Write-Host "  停止服务: taskkill /F /IM java.exe" -ForegroundColor Gray
Write-Host "  查看进程: tasklist | findstr java" -ForegroundColor Gray
