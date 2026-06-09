@echo off
cd /d D:\B.C.Sports\actions-runner\_work\BC_Sports\BC_Sports\project
start "BC Sports Admin" /B javaw -Djava.security.properties=./tls-override.security -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.region=CN -Xms8g -Xmx16g -jar target/bc-sports-admin-1.0.0.jar --spring.profiles.active=prod
