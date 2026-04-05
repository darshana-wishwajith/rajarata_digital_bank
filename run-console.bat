@echo off
REM ============================================
REM  Rajarata Digital Bank - Console Launcher
REM ============================================
set OUT_PATH=%~dp0out

echo Compiling...
javac -d "%OUT_PATH%" -sourcepath "%~dp0src" "%~dp0src\com\rajarata\bank\Main.java"
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Launching Rajarata Digital Bank (Console)...
java -cp "%OUT_PATH%" com.rajarata.bank.Main
pause
