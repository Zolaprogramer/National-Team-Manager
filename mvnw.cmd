@echo off
setlocal enabledelayedexpansion

set "MAVEN_VERSION=3.9.6"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"
set "MAVEN_ZIP=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"

where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [mvnw] ERROR: java not found on PATH. Install JDK 21+ and set JAVA_HOME.
    exit /b 1
)

if exist "%MAVEN_BIN%" goto :run

echo [mvnw] Downloading Apache Maven %MAVEN_VERSION% -- this runs once only...
if not exist "%MAVEN_HOME%" mkdir "%MAVEN_HOME%"

powershell -NoProfile -NonInteractive -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12;$ProgressPreference='SilentlyContinue';Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'"
if %ERRORLEVEL% neq 0 ( echo [mvnw] ERROR: Download failed. & exit /b 1 )

powershell -NoProfile -NonInteractive -Command "Expand-Archive -LiteralPath '%MAVEN_ZIP%' -DestinationPath '%MAVEN_HOME%' -Force"
if %ERRORLEVEL% neq 0 ( echo [mvnw] ERROR: Extraction failed. & exit /b 1 )

for /d %%D in ("%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%") do (
    if exist "%%D\bin\mvn.cmd" robocopy "%%D" "%MAVEN_HOME%" /E /MOVE /NFL /NDL /NJH /NJS >nul 2>&1
)

del /q "%MAVEN_ZIP%" 2>nul
echo [mvnw] Maven %MAVEN_VERSION% cached at %MAVEN_HOME%

:run
"%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%
