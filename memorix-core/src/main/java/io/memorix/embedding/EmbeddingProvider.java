package io.memorix.embedding;

/**
 * Interface for embedding generation.
 * 
 * <p>Converts text to vector embeddings for semantic search.
 * 
 * <p>Implementations:
 * <ul>
 *   <li>OpenAIEmbeddingProvider - OpenAI API</li>
 *   <li>OllamaEmbeddingProvider - Local Ollama models</li>
 *   <li>CustomEmbeddingProvider - User-defined</li>
 * </ul>
 */
public interface EmbeddingProvider {
    
    /**
     * Generate embedding for text.
     * 
     * @param text Text to embed
     * @return Embedding vector
     * @throws io.memorix.exception.EmbeddingException if generation fails
     */
    float[] embed(String text);
    
    /**
     * Get embedding dimension.
     * 
     * @return Dimension (e.g., 1536 for OpenAI text-embedding-3-small)
     */
    int getDimension();
    
    /**
     * Get provider name.
     * 
     * @return Provider name (e.g., "openai", "ollama")
     */
    String getProviderName();
}

