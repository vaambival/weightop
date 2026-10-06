@echo off
setlocal EnableDelayedExpansion

REM ============================================
REM  Smoke test for Comments API
REM  Requires: curl (built into Windows 10+)
REM  Assumes: containers are running
REM ============================================

if not defined BASE_URL set BASE_URL=http://localhost:8080
set API_URL=%BASE_URL%/api/v1

echo Smoke test: %API_URL%
echo.

set FAILED=0

REM Создаём временные файлы с телами запросов
set "TMP_DIR=%TEMP%\weightop-smoke"
if exist "%TMP_DIR%" rmdir /s /q "%TMP_DIR%"
mkdir "%TMP_DIR%"

> "%TMP_DIR%\create.json" echo {"author":1,"postId":100,"text":"Smoke test"}
> "%TMP_DIR%\update.json" echo {"text":"Updated text"}

goto :main

REM ---------------- Helper ----------------
REM call :check expected method url [body_file]
:check
set "EXPECTED=%~1"
set "METHOD=%~2"
set "URL=%~3"
set "BODY_FILE=%~4"

if "%BODY_FILE%"=="" (
    for /f "delims=" %%S in ('curl -s -o nul -w "%%{http_code}" -X %METHOD% "%URL%" -H "accept: application/json"') do set "CODE=%%S"
) else (
    for /f "delims=" %%S in ('curl -s -o nul -w "%%{http_code}" -X %METHOD% "%URL%" -H "accept: application/json" -H "Content-Type: application/json" -d @"%BODY_FILE%"') do set "CODE=%%S"
)

if "!CODE!"=="%EXPECTED%" (
    echo [OK ] %METHOD% %URL% -^> !CODE!
) else (
    echo [FAIL] %METHOD% %URL% -^> expected %EXPECTED%, got !CODE!
    set FAILED=1
)
exit /b 0

REM ---------------- Main ----------------
:main

REM Read endpoints
call :check 200 GET "%API_URL%/posts/100/comments"
call :check 200 GET "%API_URL%/posts/999999999/comments"

REM Not found
call :check 404 GET "%API_URL%/comments/999999999"

REM Update not found — PUT требует Content-Type и тело
call :check 404 PUT "%API_URL%/comments/999999999" "%TMP_DIR%\update.json"

REM Likes not found
call :check 404 POST "%API_URL%/comments/999999999/like"
call :check 404 DELETE "%API_URL%/comments/999999999/like"

REM Delete idempotency
call :check 204 DELETE "%API_URL%/comments/999999999"

REM Create — POST требует Content-Type и тело
call :check 201 POST "%API_URL%/comments" "%TMP_DIR%\create.json"

REM Cleanup
rmdir /s /q "%TMP_DIR%"

echo.
if "%FAILED%"=="1" (
    echo Smoke test FAILED
    exit /b 1
)
echo Smoke test passed
exit /b 0