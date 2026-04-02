@echo off
setlocal
rem 使用與本 bat 同層目錄的 JDK 17（資料夾名稱可改為 jdk17 或 jdk-17）
set "BAT_DIR=%~dp0"
set "BAT_DIR=%BAT_DIR:~0,-1%"
set "JAVA_HOME=%BAT_DIR%\jdk17"

if not exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_HOME=%BAT_DIR%\jdk-17"
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: 找不到 JDK 17。請在與 run.bat 同層建立 jdk17 或 jdk-17 資料夾並放入 JDK 17。
    echo 預期路徑: %BAT_DIR%\jdk17\bin\java.exe
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo 使用 JAVA_HOME=%JAVA_HOME%
"%JAVA_HOME%\bin\java" -version
echo.

cd /d "%BAT_DIR%"
set "JAR_FILE=%BAT_DIR%\build\libs\Sin26DataSync-1.0.0-all.jar"
if not exist "%JAR_FILE%" (
    echo 找不到 JAR，正在建置 fat jar...
    call gradlew.bat fatJar
)
if not exist "%JAR_FILE%" (
    echo ERROR: 建置失敗，找不到 %JAR_FILE%
    exit /b 1
)
echo 執行: %JAR_FILE%
echo.
"%JAVA_HOME%\bin\java" -jar "%JAR_FILE%"
endlocal
