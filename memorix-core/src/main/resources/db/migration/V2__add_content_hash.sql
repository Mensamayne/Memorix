-- Add content_hash column for deduplication
-- Migration V2: Hash-based duplicate detection

-- Add content_hash column
ALTER TABLE memories ADD COLUMN content_hash VARCHAR(64);

-- Create index for fast duplicate lookup
-- Unique constraint on (user_id, content_hash) to prevent duplicates at DB level
CREATE INDEX idx_memories_user_content_hash ON memories(user_id, content_hash) 
WHERE content_hash IS NOT NULL;

-- Comment for documentation
COMMENT ON COLUMN memories.content_hash IS 'SHA-256 hash of content for duplicate detection';

