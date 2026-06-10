@echo off
title ZPL Print Bridge
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0zpl-bridge.ps1"
pause
