@echo off
if "%OS%" == "Windows_NT" setlocal
title 文件服务器

rem ---------------------------------------------------------------------------
rem Start Static-server
rem
rem $Id: startup.bat $
rem ---------------------------------------------------------------------------

SET PROJ_HOME=%JAVA_HOME%

if exist "%PROJ_HOME%\bin\statics.exe" goto exec

if not exist "%PROJ_HOME%\bin\java.exe" goto end

copy "%PROJ_HOME%\bin\java.exe" "%PROJ_HOME%\bin\statics.exe"

:exec
rem jdk pakage
SET CLASS_PATH="%PROJ_HOME%/lib/dt.jar";"%PROJ_HOME%/lib/tools.jar"

rem container pakage
SET CLASS_PATH=%CLASS_PATH%;./lib/statics.jar
SET CLASS_PATH=%CLASS_PATH%;./lib/commons-logging.jar
SET CLASS_PATH=%CLASS_PATH%;./lib/jmagick.jar
SET CLASS_PATH=%CLASS_PATH%;./lib/log4j.jar
SET CLASS_PATH=%CLASS_PATH%;./lib/picocontainer-1.3.jar
SET CLASS_PATH=%CLASS_PATH%;./lib/je-4.0.103.jar
SET CLASS_PATH=%CLASS_PATH%;./lib/xlightweb.jar
SET CLASS_PATH=%CLASS_PATH%;./lib/xSocket.jar
SET CLASS_PATH=%CLASS_PATH%;./lib/gson-1.5.jar
SET CLASS_PATH=%CLASS_PATH%;./lib/xSocket-multiplexed-2.1.7.jar

rem debug
rem SET DEBUG_CONFIG=-Xdebug -Xrunjdwp:transport=dt_socket,address=22007,server=y,suspend=n

"%PROJ_HOME%\bin\statics" -server -Xms64m -Xmx1024m %DEBUG_CONFIG% -classpath %CLASS_PATH% com.ly.statics.server.Main

echo ===========================
echo 执行完成
echo ===========================

:end

pause
