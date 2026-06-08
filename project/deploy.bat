@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

REM ============================================
REM  BC体育后台管理系统 - 一键部署脚本
REM  双击运行即可完成：拉代码 → 构建 → 部署
REM ============================================

set JAR_NAME=bc-sports-admin-1.0.0.jar
set PORT=8080
set TLS_CONFIG=tls-override.security
set PROFILE=prod
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.region=CN

echo.
echo  ============================================
echo   BC体育后台管理系统 - 一键部署
echo  ============================================
echo.

REM ---------- 第0步：检查前置条件 ----------
echo  [检查] 验证运行环境...

where git >nul 2>&1
if %errorlevel% neq 0 (
    echo  [错误] 未找到 git，请先安装 Git
    goto :fail
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo  [错误] 未找到 mvn，请先安装 Maven 并加入 PATH
    goto :fail
)

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo  [错误] 未找到 node，请先安装 Node.js
    goto :fail
)

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo  [错误] 未找到 java，请先安装 JDK
    goto :fail
)

echo  [检查] 运行环境 OK
echo.

REM ---------- 第1步：拉取最新代码 ----------
echo  [1/5] 拉取最新代码...
git pull
if %errorlevel% neq 0 (
    echo  [警告] git pull 失败，可能有未提交的更改，继续使用当前代码...
) else (
    echo  [1/5] 代码已是最新
)
echo.

REM ---------- 第2步：构建前端 ----------
echo  [2/5] 构建前端...
cd frontend
call npm install
if %errorlevel% neq 0 (
    echo  [错误] npm install 失败
    cd ..
    goto :fail
)
call npm run build
if %errorlevel% neq 0 (
    echo  [错误] 前端构建失败
    cd ..
    goto :fail
)
cd ..
echo  [2/5] 前端构建完成
echo.

REM ---------- 第3步：构建后端 ----------
echo  [3/5] 构建后端 JAR...
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo  [错误] 后端构建失败
    goto :fail
)
echo  [3/5] 后端构建完成
echo.

REM ---------- 第4步：停止旧服务 ----------
echo  [4/5] 停止旧服务（端口 %PORT%）...
set KILLED=0
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%PORT% " ^| findstr "LISTENING" 2^>nul') do (
    echo        停止进程 PID: %%a
    taskkill /F /PID %%a >nul 2>&1
    set KILLED=1
)
if "!KILLED!"=="0" (
    echo        端口 %PORT% 无进程运行，跳过
) else (
    echo        等待端口释放...
    timeout /t 3 /nobreak >nul
)
echo  [4/5] 旧服务已停止
echo.

REM ---------- 第5步：启动新服务 ----------
echo  [5/5] 启动新服务...

if not exist "target\%JAR_NAME%" (
    echo  [错误] 找不到 target\%JAR_NAME%
    goto :fail
)

REM 检查 TLS 配置文件
set TLS_PARAM=
if exist "%TLS_CONFIG%" (
    set TLS_PARAM=-Djava.security.properties=./%TLS_CONFIG%
    echo        使用 TLS 配置: %TLS_CONFIG%
)

echo        启动命令: java %JAVA_OPTS% !TLS_PARAM! -jar target\%JAR_NAME% --spring.profiles.active=%PROFILE%
echo.
echo  ============================================
echo   服务启动中，请等待...
echo   日志将实时显示在下方
echo   按 Ctrl+C 可停止服务
echo  ============================================
echo.

java %JAVA_OPTS% %TLS_PARAM% -jar target\%JAR_NAME% --spring.profiles.active=%PROFILE%

REM 如果 Java 进程退出
echo.
echo  [信息] 服务已停止
pause
exit /b 0

:fail
echo.
echo  ============================================
echo   部署失败！请检查上方错误信息
echo  ============================================
echo.
pause
exit /b 1
