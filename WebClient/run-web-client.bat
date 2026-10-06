@echo off
rem Starts the Guess Market web client: a small local server (Node.js) that serves the web pages
rem and forwards their requests to the Guess Market server (Tomcat, http://localhost:8080/GuessMarket).
rem It also opens http://localhost:3000 in the default browser. Keep this window open while using the client.
title Guess Market Web Client
cd /d "%~dp0"

where node >/dev/null 2>nul
if errorlevel 1 (
    echo Node.js was not found. Please install Node.js and run this file again.
    pause
    exit /b 1
)

node server.js --open
if errorlevel 1 pause
