@echo off

:: Find the kobweb project folder, using this file's location to start form
cd /d "%~dp0"
cd ..\..

if defined KOBWEB_JAVA_HOME (
    set "java_cmd=%KOBWEB_JAVA_HOME%\bin\java"
) else if defined JAVA_HOME (
    set "java_cmd=%JAVA_HOME%\bin\java"
) else (
    set "java_cmd=java"
)

:: Run the java command with the common parameters
%java_cmd% -Dkobweb.server.environment=PROD -Dkobweb.site.layout=FULLSTACK -Dio.ktor.development=false -jar .kobweb/server/server.jar