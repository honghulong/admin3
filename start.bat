@echo off
title admin3-server

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
