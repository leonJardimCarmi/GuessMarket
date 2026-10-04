@echo off
rem Guess Market - the client of exercise 3. It connects to the server at http://localhost:8080/GuessMarket,
rem so start Tomcat with GuessMarket.war first. Several clients can run at the same time (one window each).
rem Needs Java 25: uses JAVA_HOME when it is set (Tomcat needs it too), otherwise the "java" found on the PATH.
setlocal
cd /d "%~dp0"

set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java"

rem JavaFX is not part of Java, so it comes with the client: its jars in javafx\lib, its native files in javafx\bin.
"%JAVA%" --module-path "javafx\lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -cp "GuessMarket-Client.jar;lib\*" com.guessmarket.client.ClientLauncher

if errorlevel 1 (
    echo.
    echo The client stopped with an error ^(see above^). It needs Java 25 - check with: java -version
    pause
)
