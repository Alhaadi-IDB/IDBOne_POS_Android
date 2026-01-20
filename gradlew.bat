@echo off
set DIR=%~dp0
set WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo gradle-wrapper.jar is missing. Please run 'gradle wrapper' once or let Android Studio generate it.
  exit /b 1
)

java -jar "%WRAPPER_JAR%" %*
