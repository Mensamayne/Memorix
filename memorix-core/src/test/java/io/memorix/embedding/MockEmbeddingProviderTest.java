package io.memorix.embedding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for MockEmbeddingProvider semantic similarity.
 * 
 * These tests verify that the mock provider generates embeddings
 * with reasonable semantic similarity for similar texts.
 */
class MockEmbeddingProviderTest {
    
    private final MockEmbeddingProvider provider = new MockEmbeddingProvider();
    
    @Test
    void testIdenticalTextsProduceSameEmbedding() {
        String text = "I prefer dark mode";
        
        float[] embedding1 = provider.embed(text);
        float[] embedding2 = provider.embed(text);
        
        assertArrayEquals(embedding1, embedding2, 
            "Identical texts must produce identical embeddings");
    }
    
    @Test
    void testSimilarTextsHaveHighSimilarity() {
        String text1 = "I prefer dark mode";
        String text2 = "I like dark theme";  // Similar: both about dark UI
        
        float[] embedding1 = provider.embed(text1);
        float[] embedding2 = provider.embed(text2);
        
        double similarity = cosineSimilarity(embedding1, embedding2);
        
        System.out.println("Similarity between '" + text1 + "' and '" + text2 + "': " + similarity);
        
        assertTrue(similarity > 0.5, 
            "Similar texts should have similarity > 0.5, got: " + similarity);
    }
    
    @Test
    void testPartialOverlapHasMediumSimilarity() {
        String text1 = "dark mode settings";
        String text2 = "dark theme preferences";  // Overlap: "dark"
        
        float[] embedding1 = provider.embed(text1);
        float[] embedding2 = provider.embed(text2);
        
        double similarity = cosineSimilarity(embedding1, embedding2);
        
        System.out.println("Similarity (partial overlap): " + similarity);
        
        assertTrue(similarity > 0.3, 
            "Texts with word overlap should have similarity > 0.3, got: " + similarity);
    }
    
    @Test
    void testUnrelatedTextsHaveLowSimilarity() {
        String text1 = "I prefer dark mode";
        String text2 = "The weather is sunny today";  // Completely unrelated
        
        float[] embedding1 = provider.embed(text1);
        float[] embedding2 = provider.embed(text2);
        
        double similarity = cosineSimilarity(embedding1, embedding2);
        
        System.out.println("Similarity (unrelated): " + similarity);
        
        assertTrue(similarity < 0.3, 
            "Unrelated texts should have similarity < 0.3, got: " + similarity);
    }
    
    @Test
    void testSearchScenario() {
        // Scenario from API test report
        String savedContent = "I prefer dark mode in applications";
        String searchQuery = "dark theme preferences";
        
        float[] contentEmbedding = provider.embed(savedContent);
        float[] queryEmbedding = provider.embed(searchQuery);
        
        double similarity = cosineSimilarity(contentEmbedding, queryEmbedding);
        
        System.out.println("Search scenario similarity: " + similarity);
        System.out.println("  Content: " + savedContent);
        System.out.println("  Query:   " + searchQuery);
        System.out.println("  Common words: 'dark' (1 of 3 query words)");
        
        assertTrue(similarity > 0.25, 
            "Search query should find similar content with similarity > 0.25, got: " + similarity);
    }
    
    @Test
    void testEmbeddingDimension() {
        float[] embedding = provider.embed("test");
        
        assertEquals(1536, embedding.length, 
            "Embedding dimension should match OpenAI standard");
        assertEquals(1536, provider.getDimension());
    }
    
    @Test
    void testEmbeddingIsNormalized() {
        float[] embedding = provider.embed("test text");
        
        double norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        
        assertEquals(1.0, norm, 0.01, 
            "Embedding should be normalized (unit vector)");
    }
    
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }
        
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);
        
        if (normA == 0 || normB == 0) {
            return 0;
        }
        
        return dotProduct / (normA * normB);
    }
}

