@echo off
rem Starts the GWR assistant (Claude Code in this folder).
rem GWR_API_BASE_URL: set it before starting to use another GWR server; otherwise the local test server is used.
if "%GWR_API_BASE_URL%"=="" set "GWR_API_BASE_URL=http://localhost:8080/gwr-api"
cd /d "%~dp0"
claude
