@echo off
chcp 65001 > nul
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
title JobSeeker Backend
cd /d D:\04_GitHub\JobSeeker
echo Starting Spring Boot backend...
mvn -DskipTests spring-boot:run
pause
