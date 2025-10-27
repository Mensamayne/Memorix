#!/bin/bash
# Memorix Docker Management Script (Unix/Linux/Mac)
# Usage: ./memorix-docker.sh [start|stop|restart|status|logs|clean|reset|build|test]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Helper functions
print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_info() { echo -e "${CYAN}ℹ️  $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_header() { echo -e "\n${MAGENTA}🧠 $1${NC}\n"; }

# Check if Docker is running
check_docker() {
    if ! docker info &> /dev/null; then
        print_error "Docker nie działa! Uruchom Docker Desktop."
        exit 1
    fi
}

# Get container status
get_container_status() {
    local container_name=$1
    local status=$(docker ps -a --filter "name=$container_name" --format "{{.Status}}" 2>/dev/null)
    
    if [ -z "$status" ]; then
        echo "not-exist"
    elif [[ $status == *"Up"* ]]; then
        echo "running"
    else
        echo "stopped"
    fi
}

# Get container health
get_container_health() {
    local container_name=$1
    docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "unknown"
}

# Start Memorix
start_memorix() {
    print_header "Starting Memorix..."
    
    local postgres_status=$(get_container_status "memorix-postgres")
    local app_status=$(get_container_status "memorix-app")
    
    if [ "$postgres_status" = "running" ] && [ "$app_status" = "running" ]; then
        print_warning "Memorix już działa!"
        show_status
        return
    fi
    
    if [ "$postgres_status" = "stopped" ] || [ "$app_status" = "stopped" ]; then
        print_info "Znaleziono zatrzymane kontenery, uruchamiam ponownie..."
        docker-compose up -d
    else
        print_info "Tworzę i uruchamiam kontenery..."
        docker-compose up -d --build
    fi
    
    print_info "Czekam na PostgreSQL..."
    local waited=0
    local max_wait=30
    while [ $waited -lt $max_wait ]; do
        local health=$(get_container_health "memorix-postgres")
        if [ "$health" = "healthy" ]; then
            print_success "PostgreSQL gotowy!"
            break
        fi
        sleep 1
        waited=$((waited + 1))
        echo -n "."
    done
    echo ""
    
    if [ $waited -ge $max_wait ]; then
        print_error "PostgreSQL timeout!"
        exit 1
    fi
    
    print_info "Czekam na Memorix App..."
    waited=0
    max_wait=60
    while [ $waited -lt $max_wait ]; do
        local health=$(get_container_health "memorix-app")
        if [ "$health" = "healthy" ]; then
            print_success "Memorix App gotowy!"
            break
        fi
        sleep 2
        waited=$((waited + 2))
        echo -n "."
    done
    echo ""
    
    print_success "Memorix uruchomiony!"
    echo ""
    print_info "🌐 Application: http://localhost:8080"
    print_info "🗄️  PostgreSQL:  localhost:5432"
    print_info "📊 Health:      http://localhost:8080/actuator/health"
}

# Stop Memorix
stop_memorix() {
    print_header "Stopping Memorix..."
    
    local postgres_status=$(get_container_status "memorix-postgres")
    local app_status=$(get_container_status "memorix-app")
    
    if [ "$postgres_status" = "not-exist" ] && [ "$app_status" = "not-exist" ]; then
        print_warning "Memorix nie jest uruchomiony."
        return
    fi
    
    print_info "Zatrzymuję kontenery..."
    docker-compose down
    
    print_success "Memorix zatrzymany!"
}

# Restart Memorix
restart_memorix() {
    print_header "Restarting Memorix..."
    
    print_info "Zatrzymuję..."
    docker-compose down
    
    print_info "Uruchamiam ponownie..."
    sleep 2
    
    start_memorix
}

# Show status
show_status() {
    print_header "Memorix Status"
    
    local postgres_status=$(get_container_status "memorix-postgres")
    local app_status=$(get_container_status "memorix-app")
    
    echo -e "${CYAN}📦 Containers:${NC}"
    
    # PostgreSQL
    echo -n "  • PostgreSQL: "
    if [ "$postgres_status" = "running" ]; then
        local health=$(get_container_health "memorix-postgres")
        echo -e "${GREEN}RUNNING ($health)${NC}"
    elif [ "$postgres_status" = "stopped" ]; then
        echo -e "${YELLOW}STOPPED${NC}"
    else
        echo -e "${RED}NOT CREATED${NC}"
    fi
    
    # Memorix App
    echo -n "  • Memorix App: "
    if [ "$app_status" = "running" ]; then
        local health=$(get_container_health "memorix-app")
        echo -e "${GREEN}RUNNING ($health)${NC}"
    elif [ "$app_status" = "stopped" ]; then
        echo -e "${YELLOW}STOPPED${NC}"
    else
        echo -e "${RED}NOT CREATED${NC}"
    fi
    
    # URLs
    if [ "$app_status" = "running" ]; then
        echo -e "\n${CYAN}🌐 URLs:${NC}"
        echo "  • Application: http://localhost:8080"
        echo "  • Health:      http://localhost:8080/actuator/health"
        echo "  • PostgreSQL:  localhost:5432"
        
        # Try to get health
        if curl -s http://localhost:8080/actuator/health &> /dev/null; then
            echo -e "\n${GREEN}💚 Health Check: OK${NC}"
        else
            echo -e "\n${YELLOW}⏳ Health Check: Application starting...${NC}"
        fi
    fi
    
    # Volumes
    echo -e "\n${CYAN}💾 Volumes:${NC}"
    local volumes=$(docker volume ls --filter "name=memorix" --format "{{.Name}}")
    if [ -n "$volumes" ]; then
        echo "$volumes" | while read vol; do
            echo "  • $vol"
        done
    else
        echo -e "  ${NC}• No volumes found${NC}"
    fi
}

# Show logs
show_logs() {
    print_header "Memorix Logs (Ctrl+C to exit)"
    
    local app_status=$(get_container_status "memorix-app")
    if [ "$app_status" != "running" ]; then
        print_warning "Memorix App nie działa. Użyj 'start' najpierw."
        return
    fi
    
    docker-compose logs -f memorix-app
}

# Clean
clean_memorix() {
    print_header "Cleaning Memorix..."
    
    print_warning "To usunie kontenery, ale ZACHOWA dane w volumes!"
    read -p "Kontynuować? (y/N): " confirm
    
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        print_info "Anulowano."
        return
    fi
    
    print_info "Zatrzymuję i usuwam kontenery..."
    docker-compose down
    
    print_success "Kontenery usunięte! Volumes zachowane."
    print_info "Użyj 'reset' aby usunąć również dane."
}

# Reset (delete volumes)
reset_memorix() {
    print_header "Resetting Memorix..."
    
    print_warning "To usunie WSZYSTKO włącznie z danymi w bazie!"
    print_warning "Volumes: memorix_postgres_data, memorix_logs"
    read -p "NA PEWNO kontynuować? (yes/N): " confirm
    
    if [ "$confirm" != "yes" ]; then
        print_info "Anulowano."
        return
    fi
    
    print_info "Zatrzymuję kontenery..."
    docker-compose down
    
    print_info "Usuwam volumes..."
    docker volume rm memorix_postgres_data -f 2>/dev/null || true
    docker volume rm memorix_logs -f 2>/dev/null || true
    
    print_success "Memorix zresetowany! Wszystkie dane usunięte."
    print_info "Użyj 'start' aby uruchomić od nowa."
}

# Build
build_memorix() {
    print_header "Building Memorix..."
    
    print_info "Buduję obraz Docker..."
    docker-compose build memorix-app
    
    print_success "Build ukończony!"
}

# Run tests
test_memorix() {
    print_header "Running Memorix Tests..."
    
    print_info "Uruchamiam testy Maven..."
    cd memorix-core
    mvn test
    
    if [ $? -eq 0 ]; then
        print_success "Wszystkie testy przeszły!"
        print_info "Coverage report: target/site/jacoco/index.html"
    else
        print_error "Testy failują! Sprawdź logi powyżej."
        exit 1
    fi
}

# Main
cat << "EOF"

╔══════════════════════════════════════════════════════╗
║                                                      ║
║       🧠  MEMORIX - Docker Management  🧠            ║
║                                                      ║
║  AI Memory Framework for Java                        ║
║  v1.1.0-SNAPSHOT                                     ║
║                                                      ║
╚══════════════════════════════════════════════════════╝

EOF

check_docker

COMMAND=${1:-status}

case $COMMAND in
    start)   start_memorix ;;
    stop)    stop_memorix ;;
    restart) restart_memorix ;;
    status)  show_status ;;
    logs)    show_logs ;;
    clean)   clean_memorix ;;
    reset)   reset_memorix ;;
    build)   build_memorix ;;
    test)    test_memorix ;;
    *)       
        print_error "Unknown command: $COMMAND"
        echo "Usage: ./memorix-docker.sh [start|stop|restart|status|logs|clean|reset|build|test]"
        exit 1
        ;;
esac

echo -e "\n💡 Dostępne komendy: start, stop, restart, status, logs, clean, reset, build, test\n"

