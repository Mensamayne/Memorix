package io.memorix.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Mock embedding provider for testing and development.
 * 
 * <p>Generates word-based embeddings with semantic similarity.
 * Each word contributes to the final embedding, so texts with
 * common words will have higher similarity.
 * 
 * <p>Activated ONLY when:
 * - memorix.embedding.provider=mock (explicitly set)
 * 
 * <p>Default provider is now OpenAI.
 */
@Component
@ConditionalOnProperty(name = "memorix.embedding.provider", havingValue = "mock")
public class MockEmbeddingProvider implements EmbeddingProvider {
    
    private static final Logger log = LoggerFactory.getLogger(MockEmbeddingProvider.class);
    private static final int DIMENSION = 1536;  // OpenAI standard
    
    @Override
    public float[] embed(String text) {
        log.debug("Generating embedding for text: '{}'", text);
        
        if (text == null || text.isEmpty()) {
            log.debug("Empty text, returning zero vector");
            return new float[DIMENSION];
        }
        
        // Tokenize text into words
        String[] words = text.toLowerCase().split("\\W+");
        
        if (words.length == 0) {
            return new float[DIMENSION];
        }
        
        // Generate embedding as average of word embeddings
        float[] embedding = new float[DIMENSION];
        
        for (String word : words) {
            if (word.isEmpty()) continue;
            
            // Generate deterministic vector for this word
            float[] wordEmbedding = generateWordEmbedding(word);
            
            // Add to cumulative embedding
            for (int i = 0; i < DIMENSION; i++) {
                embedding[i] += wordEmbedding[i];
            }
        }
        
        // Average by number of words
        int wordCount = words.length;
        for (int i = 0; i < DIMENSION; i++) {
            embedding[i] /= wordCount;
        }
        
        // Normalize to unit vector
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }
        
        log.debug("Generated embedding from {} words, first 3 values=[{}, {}, {}]", 
            wordCount, embedding[0], embedding[1], embedding[2]);
        
        return embedding;
    }
    
    /**
     * Generate deterministic embedding for a single word.
     */
    private float[] generateWordEmbedding(String word) {
        // Use word's hashCode as seed for deterministic randomness
        Random wordRandom = new Random(word.hashCode());
        float[] embedding = new float[DIMENSION];
        
        for (int i = 0; i < DIMENSION; i++) {
            embedding[i] = (float) wordRandom.nextGaussian();
        }
        
        return embedding;
    }
    
    @Override
    public int getDimension() {
        return DIMENSION;
    }
    
    @Override
    public String getProviderName() {
        return "mock";
    }
}

