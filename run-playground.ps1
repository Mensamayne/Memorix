# Memorix Playground Launcher

Write-Host "`n╔══════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                                                      ║" -ForegroundColor Cyan
Write-Host "║      🧠  MEMORIX PLAYGROUND  🧠                     ║" -ForegroundColor Cyan
Write-Host "║                                                      ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

Write-Host "🚀 Starting Memorix Backend...`n" -ForegroundColor Green

# Check if PostgreSQL is running
Write-Host "📊 Checking PostgreSQL..." -ForegroundColor Yellow

# Check if PostgreSQL process is running
$pgRunning = Get-Process postgres -ErrorAction SilentlyContinue

# Check if PostgreSQL is accessible on port 5432
$pgAccessible = $false
try {
    $tcpConnection = Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet -WarningAction SilentlyContinue
    $pgAccessible = $tcpConnection
} catch {
    $pgAccessible = $false
}

if ($pgAccessible) {
    Write-Host "✅ PostgreSQL is running and accessible`n" -ForegroundColor Green
} else {
    Write-Host "❌ PostgreSQL is not running`n" -ForegroundColor Red
    
    # Ask user if they want to start it with Docker
    $response = Read-Host "Would you like to start PostgreSQL with Docker? (Y/N)"
    
    if ($response -eq "Y" -or $response -eq "y") {
        Write-Host "`n🐳 Starting PostgreSQL with Docker...`n" -ForegroundColor Cyan
        
        # Check if Docker is running
        try {
            $dockerInfo = docker info 2>&1
            if ($LASTEXITCODE -ne 0) {
                throw "Docker not accessible"
            }
        } catch {
            Write-Host "❌ Docker is not running or not installed!`n" -ForegroundColor Red
            Write-Host "   Please install Docker Desktop: https://www.docker.com/products/docker-desktop`n" -ForegroundColor Yellow
            Write-Host "   Or start Docker Desktop if it's already installed.`n" -ForegroundColor Yellow
            exit 1
        }
        
        Write-Host "✅ Docker is running`n" -ForegroundColor Green
        
        # Check if pgvector image exists locally
        Write-Host "🔍 Checking for pgvector image..." -ForegroundColor Yellow
        $imageExists = docker images pgvector/pgvector --format "{{.Repository}}" | Select-String "pgvector"
        
        if (-not $imageExists) {
            Write-Host "📥 Downloading pgvector/pgvector:pg16 image (this may take a few minutes)...`n" -ForegroundColor Yellow
            docker pull pgvector/pgvector:pg16
            
            if ($LASTEXITCODE -ne 0) {
                Write-Host "❌ Failed to pull image!`n" -ForegroundColor Red
                exit 1
            }
            Write-Host "✅ Image downloaded successfully`n" -ForegroundColor Green
        } else {
            Write-Host "✅ pgvector image already available locally`n" -ForegroundColor Green
        }
        
        # Check if container already exists
        $containerExists = docker ps -a --filter "name=memorix-postgres" --format "{{.Names}}" | Select-String "memorix-postgres"
        
        if ($containerExists) {
            Write-Host "🔄 Existing container found. Starting it...`n" -ForegroundColor Yellow
            docker start memorix-postgres
            
            if ($LASTEXITCODE -ne 0) {
                Write-Host "⚠️  Failed to start existing container. Removing and creating new one...`n" -ForegroundColor Yellow
                docker rm -f memorix-postgres 2>&1 | Out-Null
                $containerExists = $false
            }
        }
        
        if (-not $containerExists) {
            Write-Host "🚀 Creating new PostgreSQL container...`n" -ForegroundColor Yellow
            docker run -d `
                --name memorix-postgres `
                -e POSTGRES_PASSWORD=postgres `
                -e POSTGRES_DB=memorix `
                -p 5432:5432 `
                pgvector/pgvector:pg16
            
            if ($LASTEXITCODE -ne 0) {
                Write-Host "❌ Failed to create container!`n" -ForegroundColor Red
                exit 1
            }
        }
        
        Write-Host "✅ PostgreSQL container is running`n" -ForegroundColor Green
        Write-Host "⏳ Waiting for PostgreSQL to be ready (5 seconds)...`n" -ForegroundColor Yellow
        Start-Sleep -Seconds 5
        Write-Host "✅ PostgreSQL should be ready now`n" -ForegroundColor Green
        
    } else {
        Write-Host "`n⚠️  Cannot continue without PostgreSQL.`n" -ForegroundColor Red
        Write-Host "   Options:`n" -ForegroundColor Yellow
        Write-Host "   1. Install PostgreSQL manually" -ForegroundColor Yellow
        Write-Host "   2. Use Docker (run this script again and choose Y)" -ForegroundColor Yellow
        Write-Host "`n" -ForegroundColor Yellow
        exit 1
    }
}

# Check if secrets.yml exists
if (-not (Test-Path ".\secrets.yml")) {
    Write-Host "⚠️  secrets.yml not found!" -ForegroundColor Red
    Write-Host "   Creating from template...`n" -ForegroundColor Yellow
    
    @"
memorix:
  embedding:
    openai:
      api-key: YOUR_OPENAI_API_KEY_HERE
"@ | Out-File -FilePath ".\secrets.yml" -Encoding UTF8
    
    Write-Host "✅ Created secrets.yml template" -ForegroundColor Green
    Write-Host "⚠️  Please edit secrets.yml and add your OpenAI API key!`n" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Configuration files present`n" -ForegroundColor Green

# Start Spring Boot
Write-Host "🚀 Launching Memorix...`n" -ForegroundColor Green
Write-Host "   Backend API: http://localhost:8080/api/memorix" -ForegroundColor Cyan
Write-Host "   Playground:  http://localhost:8080/playground/index.html`n" -ForegroundColor Cyan

# Find memorix root directory intelligently
$currentDir = Get-Location

# Check if we're already in memorix-core
if ($currentDir.Path -match "memorix-core$") {
    # Already in memorix-core, stay here
    Write-Host "📁 Already in memorix-core: $currentDir`n" -ForegroundColor Cyan
} elseif (Test-Path ".\memorix-core\pom.xml") {
    # We're in memorix root, go to memorix-core
    Set-Location .\memorix-core
    Write-Host "📁 Switched to: $(Get-Location)`n" -ForegroundColor Cyan
} elseif (Test-Path ".\pom.xml") {
    # We're in memorix-core (has pom.xml but not memorix-core subfolder)
    Write-Host "📁 Already in correct directory: $(Get-Location)`n" -ForegroundColor Cyan
} else {
    # Try to find memorix-core from current location
    Write-Host "❌ Cannot find pom.xml!`n" -ForegroundColor Red
    Write-Host "   Current location: $currentDir`n" -ForegroundColor Yellow
    Write-Host "   Expected either:`n" -ForegroundColor Yellow
    Write-Host "   - C:\Path\to\memorix (with memorix-core subfolder)`n" -ForegroundColor Yellow
    Write-Host "   - C:\Path\to\memorix\memorix-core (with pom.xml)`n" -ForegroundColor Yellow
    exit 1
}

mvn spring-boot:run

Write-Host "`n✅ Memorix stopped." -ForegroundColor Yellow

