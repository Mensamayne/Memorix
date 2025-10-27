package io.memorix.util;

import org.springframework.stereotype.Component;

/**
 * Fast approximate token counter.
 * 
 * <p>Uses heuristic: ~3 characters per token
 * <ul>
 *   <li>English: ~4 chars/token</li>
 *   <li>Polish: ~2 chars/token (more multi-char letters)</li>
 *   <li>Average: ~3 chars/token</li>
 * </ul>
 * 
 * <p>Advantages:
 * <ul>
 *   <li>Instant calculation</li>
 *   <li>Good enough for limits</li>
 *   <li>Can be cached in database</li>
 * </ul>
 * 
 * <p>Disadvantages:
 * <ul>
 *   <li>Not exact (±20% variance)</li>
 *   <li>Language dependent</li>
 * </ul>
 */
@Component
public class ApproximateTokenCounter implements TokenCounter {
    
    private static final int CHARS_PER_TOKEN = 3;
    
    @Override
    public int count(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        return Math.max(1, text.length() / CHARS_PER_TOKEN);
    }
}

