@echo off
cd /d C:\Users\1\Desktop\web-admin
start "WebAdmin-Node" /B node server.js
timeout /t 3 /nobreak >nul
start "WebAdmin-Ngrok" /B C:\Users\1\Desktop\ngrok\ngrok.exe http 5000 --domain=resistive-diotic-jolie.ngrok-free.dev
echo Started node + ngrok
