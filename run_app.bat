@echo off
echo Checking for connected devices...
"C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices

echo.
echo Installing APK...
"C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% NEQ 0 (
    echo Installation failed. Please ensure a device is connected.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Launching App...
"C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.textenhancer/.MainActivity

echo.
echo Done.
pause
