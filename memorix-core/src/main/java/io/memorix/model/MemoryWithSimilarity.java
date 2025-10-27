package io.memorix.model;

/**
 * Memory with similarity score.
 * 
 * <p>Used for semantic deduplication to return both the memory
 * and its similarity score to the query.
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
    
    @Override
    public String toString() {
        return "MemoryWithSimilarity{" +
                "memoryId=" + (memory != null ? memory.getId() : "null") +
                ", similarity=" + String.format("%.3f", similarity) +
                '}';
    }
}

