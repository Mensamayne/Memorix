package io.memorix.util;

/**
 * Interface for counting tokens in text.
 * 
 * <p>Used for LLM context management to ensure queries fit within token limits.
 * 
 * <p>Implementations:
 * <ul>
 *   <li>ApproximateTokenCounter - Fast, length-based approximation</li>
 *   <li>TiktokenCounter - Exact OpenAI token count (future)</li>
 * </ul>
 */
public interface TokenCounter {
    
    /**
     * Count tokens in text.
     * 
     * @param text Text to count (null/empty returns 0)
     * @return Number of tokens
     */
    int count(String text);
}

