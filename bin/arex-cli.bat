@echo off

REM ----------------------------------------------------------------------------
REM  program : arex bat
REM     date : 2022-3-16
REM  version : 0.1 io.arex.standalone.cli.ArexCli
REM ----------------------------------------------------------------------------

set JAR_NAME="arex-cli.jar"
set JAR_PATH="../arex-cli/target/"

if exist "%JAR_NAME%" (
  java -jar %JAR_NAME%
) else (
  if not exist "%JAR_PATH%arex-cli.jar" (
      echo.
      echo Can not find arex-cli.jar under %JAR_PATH%, you can run "mvn clean install" to generate jar.
      pause
      goto :eof
  )
  java -jar %JAR_PATH%/arex-cli.jar
)

pause