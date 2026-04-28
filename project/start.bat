@echo off
REM BC体育后台管理系统启动脚本
REM 设置JVM参数确保UTF-8编码

set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.region=CN

echo =================================
echo BC体育后台管理系统启动中...
echo 使用UTF-8编码配置
echo =================================

java %JAVA_OPTS% -jar target/bc-sports-admin-1.0.0.jar

pause