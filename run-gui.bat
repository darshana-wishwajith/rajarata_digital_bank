@echo off
REM ============================================
REM  Rajarata Digital Bank - JavaFX GUI Launcher
REM ============================================
set JAVAFX_PATH=%~dp0lib\javafx-sdk-24\lib
set OUT_PATH=%~dp0out

echo Compiling...
javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls -d "%OUT_PATH%" -sourcepath "%~dp0src" "%~dp0src\com\rajarata\bank\ui\fx\BankApplication.java"
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Copying resources...
copy /Y "%~dp0src\com\rajarata\bank\ui\fx\styles.css" "%OUT_PATH%\com\rajarata\bank\ui\fx\styles.css" >nul

echo Launching Rajarata Digital Bank...
java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls --enable-native-access=javafx.graphics -cp "%OUT_PATH%" com.rajarata.bank.ui.fx.BankApplication
pause
