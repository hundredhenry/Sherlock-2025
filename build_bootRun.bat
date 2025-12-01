@echo off
setlocal enabledelayedexpansion

echo Stopping any running Gradle daemons...
call gradlew --stop

echo Building project...
call gradlew build
if errorlevel 1 exit /b 1

echo Running application...
call gradlew bootRun
if errorlevel 1 exit /b 1
