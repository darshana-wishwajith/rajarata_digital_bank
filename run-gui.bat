@echo off
REM ============================================
REM  Rajarata Digital Bank - JavaFX GUI Launcher
REM ============================================
set JAVAFX_PATH=%~dp0lib\javafx-sdk-24\lib

echo Compiling...
if not exist "out" mkdir "out"
javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls -d "out" -sourcepath "%~dp0src" "%~dp0src\com\rajarata\bank\ui\fx\BankApplication.java"
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Copying resources...
if not exist "out\com\rajarata\bank\ui\fx" mkdir "out\com\rajarata\bank\ui\fx"
copy /Y "%~dp0src\com\rajarata\bank\ui\fx\styles.css" "out\com\rajarata\bank\ui\fx\styles.css" >nul

echo Launching Rajarata Digital Bank...
java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls --enable-native-access=javafx.graphics -cp "out" com.rajarata.bank.ui.fx.BankApplication
pause
