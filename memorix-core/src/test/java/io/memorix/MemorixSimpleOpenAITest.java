package io.memorix;

import io.memorix.embedding.OpenAIEmbeddingProvider;
import io.memorix.config.EmbeddingConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SIMPLE test for OpenAI integration without database.
 * 
 * <p>Tests only:
 * - OpenAI API connectivity
 * - Embedding generation
 * - Configuration loading
 * 
 * <p>Requirements:
 * - OpenAI API key in secrets.yml
 * - Internet connection
 * 
 * <p><b>DISABLED by default</b> - requires valid OpenAI API key for CI/CD safety.
 * To enable: Remove @Disabled annotation and configure real API key via environment.
 */
@SpringBootTest(classes = MemorixSimpleOpenAITest.MinimalConfig.class)
@TestPropertySource(properties = {
    "memorix.embedding.provider=openai"
})
@Disabled("Requires valid OpenAI API key - enable manually for real API testing")
public class MemorixSimpleOpenAITest {
    
    /**
     * Minimal configuration - only what's needed for OpenAI tests.
     * No database, no storage, no full application context.
     */
    @SpringBootConfiguration
    @Import({EmbeddingConfig.class, OpenAIEmbeddingProvider.class})
    static class MinimalConfig {
        // Minimal beans for OpenAI testing - just embedding provider
    }
    
    @Autowired
    private OpenAIEmbeddingProvider embeddingProvider;
    
    @Autowired
    private EmbeddingConfig config;
    
    @Test
    public void testOpenAIConfiguration() {
        // Test configuration loading
        assertThat(config).isNotNull();
        assertThat(config.getOpenai()).isNotNull();
        assertThat(config.getOpenai().getApiKey()).isNotBlank();
        assertThat(config.getOpenai().getModel()).isEqualTo("text-embedding-3-small");
        assertThat(config.getOpenai().getBaseUrl()).isEqualTo("https://api.openai.com/v1");
        
        System.out.println("✅ OpenAI configuration loaded successfully");
        System.out.println("   Model: " + config.getOpenai().getModel());
        System.out.println("   Base URL: " + config.getOpenai().getBaseUrl());
        System.out.println("   API Key: " + config.getOpenai().getApiKey().substring(0, 10) + "...");
    }
    
    @Test
    public void testOpenAIEmbeddingGeneration() {
        // Test actual OpenAI API call
        String testText = "User prefers Italian food, especially pizza and pasta";
        
        long startTime = System.currentTimeMillis();
        float[] embedding = embeddingProvider.embed(testText);
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify embedding
        assertThat(embedding).isNotNull();
        assertThat(embedding.length).isEqualTo(1536); // OpenAI text-embedding-3-small dimension
        
        // Check if embedding is normalized (norm should be close to 1.0)
        double norm = 0;
        for (float value : embedding) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        
        System.out.println("✅ OpenAI embedding generated successfully");
        System.out.println("   Text: " + testText);
        System.out.println("   Dimension: " + embedding.length);
        System.out.println("   Norm: " + String.format("%.6f", norm));
        System.out.println("   Duration: " + duration + "ms");
        System.out.println("   First 3 values: [" + 
            String.format("%.6f", embedding[0]) + ", " +
            String.format("%.6f", embedding[1]) + ", " +
            String.format("%.6f", embedding[2]) + "]");
        
        // Verify it's properly normalized
        assertThat(norm).isBetween(0.95, 1.05); // Allow small tolerance
    }
    
    @Test
    public void testEmbeddingSimilarity() {
        // Test that similar texts produce similar embeddings
        String text1 = "User likes pizza and pasta";
        String text2 = "User enjoys Italian food including pizza and pasta";
        String text3 = "User prefers cars and motorcycles"; // Different topic
        
        float[] embedding1 = embeddingProvider.embed(text1);
        float[] embedding2 = embeddingProvider.embed(text2);
        float[] embedding3 = embeddingProvider.embed(text3);
        
        // Calculate cosine similarity
        double similarity12 = cosineSimilarity(embedding1, embedding2);
        double similarity13 = cosineSimilarity(embedding1, embedding3);
        double similarity23 = cosineSimilarity(embedding2, embedding3);
        
        System.out.println("✅ Embedding similarity test");
        System.out.println("   Text1: " + text1);
        System.out.println("   Text2: " + text2);
        System.out.println("   Text3: " + text3);
        System.out.println("   Similarity 1-2: " + String.format("%.6f", similarity12));
        System.out.println("   Similarity 1-3: " + String.format("%.6f", similarity13));
        System.out.println("   Similarity 2-3: " + String.format("%.6f", similarity23));
        
        // Similar texts should have higher similarity
        assertThat(similarity12).isGreaterThan(similarity13);
        assertThat(similarity12).isGreaterThan(similarity23);
        assertThat(similarity12).isGreaterThan(0.7); // High similarity for similar content
        
        // Different texts should have lower similarity
        assertThat(similarity13).isLessThan(0.8);
        assertThat(similarity23).isLessThan(0.8);
    }
    
    @Test
    public void testMultipleEmbeddings() {
        String[] texts = {
            "User works as a software engineer",
            "User lives in San Francisco",
            "User enjoys hiking on weekends",
            "User prefers vegetarian food"
        };
        
        System.out.println("✅ Multiple embeddings test");
        
        for (String text : texts) {
            long startTime = System.currentTimeMillis();
            float[] embedding = embeddingProvider.embed(text);
            long duration = System.currentTimeMillis() - startTime;
            
            assertThat(embedding).isNotNull();
            assertThat(embedding.length).isEqualTo(1536);
            
            System.out.println("   '" + text + "' -> " + duration + "ms");
        }
    }
    
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
