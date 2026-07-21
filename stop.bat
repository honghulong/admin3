@echo off
title admin3-server-stop

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1"
