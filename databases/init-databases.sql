-- Initialize Memorix databases
-- This script runs automatically when PostgreSQL container starts

-- Create memorix_docs database (for multi-datasource example)
CREATE DATABASE memorix_docs;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE memorix TO postgres;
GRANT ALL PRIVILEGES ON DATABASE memorix_docs TO postgres;

-- Note: Applications using Memorix can auto-create their databases
-- by setting: memorix.auto-create-database=true
-- No need to hardcode them here!

-- Verify databases exist
\l
