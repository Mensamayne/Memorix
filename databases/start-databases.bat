@echo off
echo Starting Memorix databases...

REM Check if Docker is running
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

REM Start databases
echo Starting PostgreSQL with pgvector...
docker-compose up -d

REM Wait for databases to be ready
echo Waiting for databases to be ready...
timeout /t 10 /nobreak >nul

REM Check if databases are running
docker-compose ps

echo.
echo Databases started successfully!
echo.
echo Available databases:
echo - memorix (main database)
echo - memorix_docs (multi-datasource example)
echo.
echo Note: Applications can auto-create their own databases
echo by setting: memorix.auto-create-database=true
echo.
echo Connection: localhost:5432
echo Username: postgres
echo Password: postgres
echo.
echo To stop databases: docker-compose down
echo.
pause
