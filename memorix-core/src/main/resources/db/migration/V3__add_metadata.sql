-- Add metadata column for storing custom properties
-- Migration V3: JSONB metadata support

-- Add metadata column (JSONB for efficient JSON storage and indexing)
ALTER TABLE memories ADD COLUMN metadata JSONB DEFAULT '{}'::jsonb;

-- Create GIN index for fast JSON queries
-- Allows efficient queries like: WHERE metadata @> '{"immutable": true}'
CREATE INDEX idx_memories_metadata ON memories USING GIN (metadata);

-- Create specific index for immutable flag (commonly used)
CREATE INDEX idx_memories_immutable ON memories ((metadata->>'immutable'))
WHERE (metadata->>'immutable') IS NOT NULL;

-- Comment for documentation
COMMENT ON COLUMN memories.metadata IS 'Custom properties stored as JSONB (e.g., immutable, source, tags)';

