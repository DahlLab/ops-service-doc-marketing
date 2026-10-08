@echo off
rem Doppelklick beendet OpsServiceDoc (siehe stop.ps1)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1"
pause
