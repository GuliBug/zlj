@echo off
chcp 65001 >nul
cd /d %~dp0frontend-web
if not exist node_modules call npm install
echo [正隆记商城] 启动 Web 管理端 http://localhost:5173 （admin/admin123）
call npm run dev
