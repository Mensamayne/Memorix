package io.memorix;

import io.memorix.embedding.OpenAIEmbeddingProvider;
import io.memorix.config.EmbeddingConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * STANDALONE test for OpenAI API without Spring Boot.
 * 
 * <p>Tests:
 * - OpenAI API connectivity
 * - Embedding generation
 * - Configuration loading
 * 
 * <p>Requirements:
 * - Valid OpenAI API key (configured via environment or secrets)
 * - Internet connection
 * 
 * <p><b>DISABLED by default</b> - requires valid OpenAI API key for CI/CD safety.
 * To enable: Remove @Disabled annotation and configure real API key via environment.
 * 
 * <p>Note: Uses mock API key in code for security - real key should be provided via environment.
 */
@Disabled("Requires valid OpenAI API key - enable manually for real API testing")
public class OpenAIStandaloneTest {
    
    @Test
    public void testOpenAIStandalone() {
        // Manual configuration with mock API key for testing
        EmbeddingConfig.OpenAIConfig openaiConfig = new EmbeddingConfig.OpenAIConfig();
        openaiConfig.setApiKey("sk-test-mock-key-for-ci-cd-testing-only");
        openaiConfig.setModel("text-embedding-3-small");
        openaiConfig.setBaseUrl("https://api.openai.com/v1");
        openaiConfig.setTimeout(30000);
        openaiConfig.setMaxRetries(3);
        
        EmbeddingConfig config = new EmbeddingConfig();
        config.setOpenai(openaiConfig);
        
        // Create provider
        OpenAIEmbeddingProvider provider = new OpenAIEmbeddingProvider(config);
        
        System.out.println("✅ OpenAI provider created successfully");
        System.out.println("   Model: " + config.getOpenai().getModel());
        System.out.println("   Base URL: " + config.getOpenai().getBaseUrl());
        System.out.println("   API Key: " + config.getOpenai().getApiKey().substring(0, 10) + "...");
        
        // Test embedding generation
        String testText = "User prefers Italian food, especially pizza and pasta";
        
        long startTime = System.currentTimeMillis();
        float[] embedding = provider.embed(testText);
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
        
        // Test similarity
        String similarText = "User likes Italian cuisine including pizza and pasta";
        float[] embedding2 = provider.embed(similarText);
        
        double similarity = cosineSimilarity(embedding, embedding2);
        
        System.out.println("✅ Similarity test");
        System.out.println("   Text1: " + testText);
        System.out.println("   Text2: " + similarText);
        System.out.println("   Similarity: " + String.format("%.6f", similarity));
        
        // Similar texts should have high similarity
        assertThat(similarity).isGreaterThan(0.8);
        
        System.out.println("🎉 ALL TESTS PASSED! OpenAI integration works perfectly!");
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
