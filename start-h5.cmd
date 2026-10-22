@echo off
chcp 65001 >nul
cd /d %~dp0frontend-h5
if not exist node_modules call npm install
echo [正隆记商城] 启动 H5 商城端 http://localhost:10086
call npm run dev
