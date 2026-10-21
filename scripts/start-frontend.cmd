@echo off
setlocal
set PATH=D:\devlopment_tools\nodejs;%PATH%
cd /d "%~dp0..\frontend"
if not exist node_modules call npm install
npm run dev
