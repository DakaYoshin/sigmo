@echo off
color 0C
title LoginServer: Console
:start

REM -------------------------------------
REM Default parameters for a basic server.
REM -------------------------------------

java -Xmx64m -XX:+UseParallelGC -XX:+AggressiveOpts -XX:ParallelGCThreads=2 -XX:MaxPermSize=64m -cp ./lib/*;L2L2jSigmo.jar com.loginserver.L2LoginServer

if ERRORLEVEL 1 goto error
goto exit
:error
echo ErrorLevel = 1 (error), please read log
:exit
pause
exit