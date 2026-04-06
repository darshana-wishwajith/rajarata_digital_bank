@echo off
REM ============================================
REM  Rajarata Digital Bank - Console Launcher
REM ============================================
echo Compiling...
if not exist "out" mkdir "out"
javac -d "out" -sourcepath "%~dp0src" "%~dp0src\com\rajarata\bank\Main.java"
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Launching Rajarata Digital Bank (Console)...
java -cp "out" com.rajarata.bank.Main
pause
