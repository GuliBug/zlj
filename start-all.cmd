@echo off
chcp 65001 >nul
echo [正隆记商城] 一键启动：后端 8080 + H5 10086 + Web 5173
start "zhenglongji-backend" cmd /c "%~dp0start-backend.cmd"
timeout /t 15 /nobreak >nul
start "zhenglongji-h5" cmd /c "%~dp0start-h5.cmd"
start "zhenglongji-web" cmd /c "%~dp0start-web.cmd"
echo 已拉起三个窗口：后端 http://localhost:8080 ｜ H5 http://localhost:10086 ｜ 管理端 http://localhost:5173
