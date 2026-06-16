@echo off
cd /d D:\BlockOffensive
set JAVA_HOME=C:\Progra~1\Microsoft\jdk-21.0.11.10-hotspot
%JAVA_HOME%\bin\java.exe -Xmx2G -classpath "D:\BlockOffensive\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain assemble --no-daemon
exit /b %ERRORLEVEL%
