package io.memorix.deduplication;

import io.memorix.model.DeduplicationConfig;
import io.memorix.model.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Hybrid deduplication detector combining hash and semantic detection.
 * 
 * <p>Two-level detection strategy for optimal performance and accuracy:
 * <ol>
 *   <li><b>Level 1 (Fast):</b> Hash-based exact duplicate detection (~1ms)</li>
 *   <li><b>Level 2 (Smart):</b> Semantic similarity detection if hash fails (~20ms)</li>
 * </ol>
 * 
 * <p>Benefits:
 * <ul>
 *   <li>Fast path for exact duplicates (instant hash lookup)</li>
 *   <li>Smart detection for paraphrases (semantic similarity)</li>
 *   <li>Cost-effective (semantic only when needed)</li>
 * </ul>
 * 
 * <p>Example flow:
 * <pre>{@code
 * save("User loves pizza")        → Level 1: hash check → NO MATCH
 *                                  → Level 2: semantic → NO MATCH → SAVE
 * 
 * save("User loves pizza")        → Level 1: hash check → MATCH! → MERGE (fast!)
 * 
 * save("User really likes pizza") → Level 1: hash check → NO MATCH (different text)
 *                                  → Level 2: semantic → MATCH! (similar meaning) → MERGE
 * }</pre>
 */
@Component
public class HybridDeduplicationDetector implements DeduplicationDetector {
    
    private static final Logger log = LoggerFactory.getLogger(HybridDeduplicationDetector.class);
    
    private final HashDeduplicationDetector hashDetector;
    private final SemanticDeduplicationDetector semanticDetector;
    
    public HybridDeduplicationDetector(HashDeduplicationDetector hashDetector,
                                      SemanticDeduplicationDetector semanticDetector) {
        this.hashDetector = hashDetector;
        this.semanticDetector = semanticDetector;
    }
    
    @Override
    public Optional<Memory> findDuplicate(String userId, String content, DeduplicationConfig config) {
        if (content == null || content.trim().isEmpty()) {
            return Optional.empty();
        }
        
        log.debug("Hybrid duplicate check: userId={}, semanticEnabled={}", 
            userId, config.isSemanticEnabled());
        
        // Level 1: Hash-based detection (fast!)
        Optional<Memory> hashDuplicate = hashDetector.findDuplicate(userId, content, config);
        if (hashDuplicate.isPresent()) {
            log.debug("Duplicate found at Level 1 (hash): id={}", hashDuplicate.get().getId());
            return hashDuplicate;
        }
        
        // Level 2: Semantic detection (if enabled)
        if (config.isSemanticEnabled()) {
            Optional<Memory> semanticDuplicate = semanticDetector.findDuplicate(userId, content, config);
            if (semanticDuplicate.isPresent()) {
                log.debug("Duplicate found at Level 2 (semantic): id={}", 
                    semanticDuplicate.get().getId());
                return semanticDuplicate;
            }
        }
        
        log.debug("No duplicate found at any level");
        return Optional.empty();
    }
}

