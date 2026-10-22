@echo off
chcp 65001 >nul
set SERVER__PORT=8080
cd /d %~dp0backend
echo [正隆记商城] 启动后端 http://localhost:8080 （H2 内存库，账号 admin/admin123）
call mvnw.cmd spring-boot:run
