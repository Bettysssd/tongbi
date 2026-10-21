@echo off
setlocal
set JAVA_HOME=D:\devlopment_tools\jdk8
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "%~dp0..\backend"
E:\jiadu\apache-maven-3.6.1\bin\mvn.cmd spring-boot:run
