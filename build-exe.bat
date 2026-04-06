@echo off
set JAVAFX_PATH=lib\javafx-sdk-24\lib

echo [1/3] Compiling Project...
if not exist "out" mkdir "out"
javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.graphics,javafx.base -d out -sourcepath src src\com\rajarata\bank\ui\fx\BankApplication.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo [2/3] Creating Manifest and JAR...
if not exist "out\com\rajarata\bank\ui\fx" mkdir "out\com\rajarata\bank\ui\fx"
copy /Y "src\com\rajarata\bank\ui\fx\styles.css" "out\com\rajarata\bank\ui\fx\styles.css" >nul
jar --create --file out/Bank.jar --main-class com.rajarata.bank.ui.fx.BankApplication -C out .

echo [3/3] Packaging Standalone EXE...
if not exist "dist" mkdir "dist"
jpackage --type exe --dest dist --name "RajarataBank" ^
         --input out --main-jar Bank.jar ^
         --module-path "%JAVAFX_PATH%" ^
         --add-modules javafx.controls,javafx.graphics,javafx.base ^
         --win-menu --win-shortcut --win-dir-chooser ^
         --vendor "Rajarata University" --description "OOP Mini Project - Digital Bank"

if errorlevel 1 (
    echo.
    echo ERROR: Deployment failed. Please ensure WiX Toolset is installed: https://wixtoolset.org/releases/
) else (
    echo.
    echo SUCCESS: Installer created in "dist" folder.
)
pause
