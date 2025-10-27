-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Main memories table
CREATE TABLE memories (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),  -- OpenAI text-embedding-3-small dimension
    decay INTEGER NOT NULL DEFAULT 100,
    importance FLOAT NOT NULL DEFAULT 0.5,
    token_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_accessed_at TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_memories_user_id ON memories(user_id);
CREATE INDEX idx_memories_user_decay ON memories(user_id, decay) WHERE decay > 0;
CREATE INDEX idx_memories_created_at ON memories(created_at);

-- HNSW index for vector similarity search (fast!)
CREATE INDEX idx_memories_embedding ON memories USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Function to automatically update token_count
CREATE OR REPLACE FUNCTION update_token_count()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.content IS NOT NULL THEN
        -- Approximate: length / 3 characters per token
        NEW.token_count = LENGTH(NEW.content) / 3;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update token_count on insert/update
CREATE TRIGGER token_count_trigger
BEFORE INSERT OR UPDATE ON memories
FOR EACH ROW
EXECUTE FUNCTION update_token_count();

-- Function to automatically update updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update updated_at
CREATE TRIGGER updated_at_trigger
BEFORE UPDATE ON memories
FOR EACH ROW
EXECUTE FUNCTION update_updated_at();

-- View for "alive" memories (decay > 0)
CREATE VIEW alive_memories AS
SELECT * FROM memories WHERE decay > 0;

-- Comments for documentation
COMMENT ON TABLE memories IS 'Main table for storing memories with embeddings';
COMMENT ON COLUMN memories.embedding IS 'Vector embedding (1536 dimensions for OpenAI)';
COMMENT ON COLUMN memories.decay IS 'Decay value (0-128), used for lifecycle management';
COMMENT ON COLUMN memories.importance IS 'Importance score (0.0-1.0)';
COMMENT ON COLUMN memories.token_count IS 'Approximate token count for LLM context management';

