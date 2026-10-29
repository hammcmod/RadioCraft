@echo off
rem Starts the RadioCraft dev client. Run "gradlew runData" yourself first when generated assets need refreshing.
cd /d "%~dp0"
call "%~dp0gradlew.bat" runClient
