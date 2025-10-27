package io.memorix.query;

import io.memorix.model.Memory;

/**
 * Memory with similarity score.
 * 
 * <p>Used internally by query executor to track similarity during search.
 */
public class MemoryWithSimilarity {
    
    private final Memory memory;
    private final double similarity;
    
    public MemoryWithSimilarity(Memory memory, double similarity) {
        this.memory = memory;
        this.similarity = similarity;
    }
    
    public Memory getMemory() {
        return memory;
    }
    
    public double getSimilarity() {
        return similarity;
    }
}

