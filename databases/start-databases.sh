#!/bin/bash

echo "Starting Memorix databases..."

# Check if Docker is running
if ! docker version > /dev/null 2>&1; then
    echo "ERROR: Docker is not running!"
    echo "Please start Docker and try again."
    exit 1
fi

# Start databases
echo "Starting PostgreSQL with pgvector..."
docker-compose up -d

# Wait for databases to be ready
echo "Waiting for databases to be ready..."
sleep 10

# Check if databases are running
docker-compose ps

echo ""
echo "Databases started successfully!"
echo ""
echo "Available databases:"
echo "- memorix (main database)"
echo "- memorix_docs (multi-datasource example)"
echo ""
echo "Note: Applications can auto-create their own databases"
echo "by setting: memorix.auto-create-database=true"
echo ""
echo "Connection: localhost:5432"
echo "Username: postgres"
echo "Password: postgres"
echo ""
echo "To stop databases: docker-compose down"
