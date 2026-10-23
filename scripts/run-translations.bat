
@echo off
setlocal
cd /d "%~dp0.."

set "PYTHON=%CD%\.translate-env\Scripts\python.exe"
set "LT=%CD%\.translate-env\Scripts\libretranslate.exe"
set "SERVER=http://127.0.0.1:5000"

echo.
echo ===== SmartIsland Translation Generator =====
echo Project: %CD%
echo.

if not exist "%PYTHON%" (
    echo Creating root Python environment...
    py -3.12 -m venv .translate-env
    if errorlevel 1 goto :error
)

if not exist "%LT%" (
    echo Installing LibreTranslate in root environment...
    "%PYTHON%" -m pip install libretranslate
    if errorlevel 1 goto :error
)

"%PYTHON%" -m pip install requests
if errorlevel 1 goto :error

if not exist "scripts\translate_strings.py" (
    echo ERROR: scripts\translate_strings.py not found.
    goto :error
)

echo Checking LibreTranslate...

"%PYTHON%" -c "import requests; requests.get('%SERVER%/languages',timeout=3).raise_for_status()" >nul 2>&1
if not errorlevel 1 goto :run_script

echo Starting LibreTranslate server...
echo A separate server window will open. Keep it open.
echo.

start "SmartIsland LibreTranslate" "%LT%" --host 127.0.0.1 --port 5000 --load-only en,pt,es,zh,ja

echo Waiting for LibreTranslate to become ready...

for /L %%i in (1,1,90) do (
    "%PYTHON%" -c "import requests; requests.get('%SERVER%/languages',timeout=3).raise_for_status()" >nul 2>&1
    if not errorlevel 1 goto :run_script
    timeout /t 2 /nobreak >nul
)

echo.
echo ERROR: LibreTranslate did not start within 180 seconds.
echo Check the separate server window for errors.
echo Ensure port 5000 is available and the language models are installed.
goto :error

:run_script
echo.
echo LibreTranslate is running!
echo Starting translation generator...
echo.

"%PYTHON%" scripts\translate_strings.py
set "RESULT=%ERRORLEVEL%"

echo.
if "%RESULT%"=="0" (
    echo Translation process finished.
) else (
    echo Translation script failed with exit code %RESULT%.
)

pause
exit /b %RESULT%

:error
echo.
echo Setup failed. Read the error messages above.
pause
exit /b 1
