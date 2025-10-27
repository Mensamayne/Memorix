# Memorix Docker Management Script
# Usage: .\memorix-docker.ps1 [start|stop|restart|status|logs|clean|reset]

param(
    [Parameter(Position=0)]
    [ValidateSet('start', 'stop', 'restart', 'status', 'logs', 'clean', 'reset', 'build', 'test')]
    [string]$Command = 'status'
)

$ErrorActionPreference = 'Stop'

# Colors
function Write-Success { param($msg) Write-Host "✅ $msg" -ForegroundColor Green }
function Write-Info { param($msg) Write-Host "ℹ️  $msg" -ForegroundColor Cyan }
function Write-Warning { param($msg) Write-Host "⚠️  $msg" -ForegroundColor Yellow }
function Write-Error-Custom { param($msg) Write-Host "❌ $msg" -ForegroundColor Red }
function Write-Header { param($msg) Write-Host "`n🧠 $msg" -ForegroundColor Magenta -BackgroundColor Black }

# Check if Docker is running
function Test-DockerRunning {
    try {
        docker info | Out-Null
        return $true
    } catch {
        Write-Error-Custom "Docker nie działa! Uruchom Docker Desktop."
        exit 1
    }
}

# Check container status
function Get-ContainerStatus {
    param([string]$containerName)
    
    $status = docker ps -a --filter "name=$containerName" --format "{{.Status}}" 2>$null
    if ($status) {
        if ($status -match "Up") {
            return "running"
        } else {
            return "stopped"
        }
    }
    return "not-exist"
}

# Get container health
function Get-ContainerHealth {
    param([string]$containerName)
    
    $health = docker inspect --format='{{.State.Health.Status}}' $containerName 2>$null
    return $health
}

# Start Memorix
function Start-Memorix {
    Write-Header "Starting Memorix..."
    
    $postgresStatus = Get-ContainerStatus "memorix-postgres"
    $appStatus = Get-ContainerStatus "memorix-app"
    
    if ($postgresStatus -eq "running" -and $appStatus -eq "running") {
        Write-Warning "Memorix już działa!"
        Show-Status
        return
    }
    
    if ($postgresStatus -eq "stopped" -or $appStatus -eq "stopped") {
        Write-Info "Znaleziono zatrzymane kontenery, uruchamiam ponownie..."
        docker-compose up -d
    } else {
        Write-Info "Tworzę i uruchamiam kontenery..."
        docker-compose up -d --build
    }
    
    Write-Info "Czekam na PostgreSQL..."
    $maxWait = 30
    $waited = 0
    while ($waited -lt $maxWait) {
        $health = Get-ContainerHealth "memorix-postgres"
        if ($health -eq "healthy") {
            Write-Success "PostgreSQL gotowy!"
            break
        }
        Start-Sleep -Seconds 1
        $waited++
        Write-Host "." -NoNewline
    }
    Write-Host ""
    
    if ($waited -ge $maxWait) {
        Write-Error-Custom "PostgreSQL timeout!"
        exit 1
    }
    
    Write-Info "Czekam na Memorix App..."
    $maxWait = 60
    $waited = 0
    while ($waited -lt $maxWait) {
        $health = Get-ContainerHealth "memorix-app"
        if ($health -eq "healthy") {
            Write-Success "Memorix App gotowy!"
            break
        }
        Start-Sleep -Seconds 2
        $waited += 2
        Write-Host "." -NoNewline
    }
    Write-Host ""
    
    Write-Success "Memorix uruchomiony!"
    Write-Info ""
    Write-Info "🌐 Application: http://localhost:8080"
    Write-Info "🗄️  PostgreSQL:  localhost:5432"
    Write-Info "📊 Health:      http://localhost:8080/actuator/health"
    Write-Info "📖 API Docs:    http://localhost:8080/actuator"
}

# Stop Memorix
function Stop-Memorix {
    Write-Header "Stopping Memorix..."
    
    $postgresStatus = Get-ContainerStatus "memorix-postgres"
    $appStatus = Get-ContainerStatus "memorix-app"
    
    if ($postgresStatus -eq "not-exist" -and $appStatus -eq "not-exist") {
        Write-Warning "Memorix nie jest uruchomiony."
        return
    }
    
    Write-Info "Zatrzymuję kontenery..."
    docker-compose down
    
    Write-Success "Memorix zatrzymany!"
}

# Restart Memorix
function Restart-Memorix {
    Write-Header "Restarting Memorix..."
    
    Write-Info "Zatrzymuję..."
    docker-compose down
    
    Write-Info "Uruchamiam ponownie..."
    Start-Sleep -Seconds 2
    
    Start-Memorix
}

# Show status
function Show-Status {
    Write-Header "Memorix Status"
    
    $postgresStatus = Get-ContainerStatus "memorix-postgres"
    $appStatus = Get-ContainerStatus "memorix-app"
    
    Write-Host "`n📦 Containers:" -ForegroundColor Cyan
    
    # PostgreSQL
    Write-Host "  • PostgreSQL: " -NoNewline
    if ($postgresStatus -eq "running") {
        $health = Get-ContainerHealth "memorix-postgres"
        Write-Host "RUNNING ($health)" -ForegroundColor Green
    } elseif ($postgresStatus -eq "stopped") {
        Write-Host "STOPPED" -ForegroundColor Yellow
    } else {
        Write-Host "NOT CREATED" -ForegroundColor Red
    }
    
    # Memorix App
    Write-Host "  • Memorix App: " -NoNewline
    if ($appStatus -eq "running") {
        $health = Get-ContainerHealth "memorix-app"
        Write-Host "RUNNING ($health)" -ForegroundColor Green
    } elseif ($appStatus -eq "stopped") {
        Write-Host "STOPPED" -ForegroundColor Yellow
    } else {
        Write-Host "NOT CREATED" -ForegroundColor Red
    }
    
    # URLs
    if ($appStatus -eq "running") {
        Write-Host "`n🌐 URLs:" -ForegroundColor Cyan
        Write-Host "  • Application: http://localhost:8080" -ForegroundColor White
        Write-Host "  • Health:      http://localhost:8080/actuator/health" -ForegroundColor White
        Write-Host "  • PostgreSQL:  localhost:5432" -ForegroundColor White
        
        # Try to get health
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
            Write-Host "`n💚 Health Check:" -ForegroundColor Green
            Write-Host "  • Status: $($health.status)" -ForegroundColor Green
        } catch {
            Write-Host "`n⏳ Health Check: Application starting..." -ForegroundColor Yellow
        }
    }
    
    # Volumes
    Write-Host "`n💾 Volumes:" -ForegroundColor Cyan
    $volumes = docker volume ls --filter "name=memorix" --format "{{.Name}}"
    if ($volumes) {
        foreach ($vol in $volumes) {
            Write-Host "  • $vol" -ForegroundColor White
        }
    } else {
        Write-Host "  • No volumes found" -ForegroundColor Gray
    }
}

# Show logs
function Show-Logs {
    Write-Header "Memorix Logs (Ctrl+C to exit)"
    
    $appStatus = Get-ContainerStatus "memorix-app"
    if ($appStatus -ne "running") {
        Write-Warning "Memorix App nie działa. Użyj 'start' najpierw."
        return
    }
    
    docker-compose logs -f memorix-app
}

# Clean everything
function Clean-Memorix {
    Write-Header "Cleaning Memorix..."
    
    Write-Warning "To usunie kontenery, ale ZACHOWA dane w volumes!"
    $confirm = Read-Host "Kontynuować? (y/N)"
    
    if ($confirm -ne 'y' -and $confirm -ne 'Y') {
        Write-Info "Anulowano."
        return
    }
    
    Write-Info "Zatrzymuję i usuwam kontenery..."
    docker-compose down
    
    Write-Success "Kontenery usunięte! Volumes zachowane."
    Write-Info "Użyj 'reset' aby usunąć również dane."
}

# Reset (delete volumes too)
function Reset-Memorix {
    Write-Header "Resetting Memorix..."
    
    Write-Warning "To usunie WSZYSTKO włącznie z danymi w bazie!"
    Write-Warning "Volumes: memorix_postgres_data, memorix_logs"
    $confirm = Read-Host "NA PEWNO kontynuować? (yes/N)"
    
    if ($confirm -ne 'yes') {
        Write-Info "Anulowano."
        return
    }
    
    Write-Info "Zatrzymuję kontenery..."
    docker-compose down
    
    Write-Info "Usuwam volumes..."
    docker volume rm memorix_postgres_data -f 2>$null
    docker volume rm memorix_logs -f 2>$null
    
    Write-Success "Memorix zresetowany! Wszystkie dane usunięte."
    Write-Info "Użyj 'start' aby uruchomić od nowa."
}

# Build application
function Build-Memorix {
    Write-Header "Building Memorix..."
    
    Write-Info "Buduję obraz Docker..."
    docker-compose build memorix-app
    
    Write-Success "Build ukończony!"
}

# Run tests
function Test-Memorix {
    Write-Header "Running Memorix Tests..."
    
    Write-Info "Uruchamiam testy Maven..."
    Push-Location memorix-core
    try {
        mvn test
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Wszystkie testy przeszły!"
            Write-Info "Coverage report: target/site/jacoco/index.html"
        } else {
            Write-Error-Custom "Testy failują! Sprawdź logi powyżej."
        }
    } finally {
        Pop-Location
    }
}

# Main
Write-Host @"

╔══════════════════════════════════════════════════════╗
║                                                      ║
║       🧠  MEMORIX - Docker Management  🧠            ║
║                                                      ║
║  AI Memory Framework for Java                        ║
║  v1.1.0-SNAPSHOT                                     ║
║                                                      ║
╚══════════════════════════════════════════════════════╝

"@ -ForegroundColor Magenta

Test-DockerRunning

switch ($Command) {
    'start'   { Start-Memorix }
    'stop'    { Stop-Memorix }
    'restart' { Restart-Memorix }
    'status'  { Show-Status }
    'logs'    { Show-Logs }
    'clean'   { Clean-Memorix }
    'reset'   { Reset-Memorix }
    'build'   { Build-Memorix }
    'test'    { Test-Memorix }
    default   { Show-Status }
}

Write-Host "`n💡 Dostępne komendy: start, stop, restart, status, logs, clean, reset, build, test`n" -ForegroundColor Gray

