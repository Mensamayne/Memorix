package io.memorix.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.memorix.config.EmbeddingConfig;
import io.memorix.exception.EmbeddingException;
import io.memorix.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI embedding provider.
 * 
 * <p>Uses OpenAI API to generate embeddings.
 * Activated when: memorix.embedding.provider=openai
 * 
 * <p>Configuration:
 * <pre>{@code
 * memorix:
 *   embedding:
 *     provider: openai
 *     openai:
 *       api-key: sk-proj-...
 *       model: text-embedding-3-small
 * }</pre>
 */
@Component
@ConditionalOnProperty(name = "memorix.embedding.provider", havingValue = "openai")
public class OpenAIEmbeddingProvider implements EmbeddingProvider {
    
    private static final Logger log = LoggerFactory.getLogger(OpenAIEmbeddingProvider.class);
    private static final int DIMENSION = 1536;  // text-embedding-3-small
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int maxRetries;
    private final int timeoutMillis;
    
    public OpenAIEmbeddingProvider(EmbeddingConfig config) {
        this.apiKey = config.getOpenai().getApiKey();
        this.model = config.getOpenai().getModel();
        this.baseUrl = config.getOpenai().getBaseUrl();
        this.maxRetries = config.getOpenai().getMaxRetries();
        this.timeoutMillis = config.getOpenai().getTimeout();
        
        // ✅ FIX: Use RestTemplate with proper timeout configuration
        // HttpClient.timeout() has issues with hanging connections
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(this.timeoutMillis);
        factory.setReadTimeout(this.timeoutMillis);  // ✅ READ timeout!
        
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
        
        log.info("OpenAI Embedding Provider initialized with RestTemplate (model: {}, dimension: {}, timeout: {}ms)", 
            model, DIMENSION, this.timeoutMillis);
    }
    
    @Override
    public float[] embed(String text) {
        // Validate API key on first use
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new EmbeddingException(ErrorCode.CONFIGURATION_ERROR,
                "OpenAI API key not configured. " +
                "Set memorix.embedding.openai.api-key in application.yml or secrets.yml")
                .withContext("provider", "openai");
        }
        
        if (text == null || text.trim().isEmpty()) {
            return new float[DIMENSION];
        }
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return callOpenAI(text);
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw new EmbeddingException(ErrorCode.EMBEDDING_GENERATION_FAILED,
                        "Failed to generate embedding after " + maxRetries + " attempts: " + e.getMessage(), e)
                        .withContext("text", text.substring(0, Math.min(100, text.length())))
                        .withContext("attempts", attempt);
                }
                
                log.warn("Embedding generation failed (attempt {}/{}): {}", 
                    attempt, maxRetries, e.getMessage());
                
                // ✅ IMPROVED: Exponential backoff with jitter
                // Attempt 1: 1s + jitter
                // Attempt 2: 2s + jitter  
                // Attempt 3: 4s + jitter
                long baseDelay = (long) Math.pow(2, attempt - 1) * 1000L;
                long jitter = (long) (Math.random() * 500); // 0-500ms jitter
                long totalDelay = baseDelay + jitter;
                
                log.debug("Retrying after {}ms (base: {}ms, jitter: {}ms)", totalDelay, baseDelay, jitter);
                
                try {
                    Thread.sleep(totalDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new EmbeddingException(ErrorCode.EMBEDDING_GENERATION_FAILED,
                        "Interrupted during retry", ie);
                }
            }
        }
        
        throw new EmbeddingException(ErrorCode.EMBEDDING_GENERATION_FAILED,
            "Should not reach here");
    }
    
    private float[] callOpenAI(String text) throws Exception {
        // Build request body
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "input", text
        );
        
        // Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // Send request with RestTemplate (has proper READ timeout!)
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/embeddings",
            request,
            String.class
        );
        
        // Check status
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new EmbeddingException(ErrorCode.EMBEDDING_GENERATION_FAILED,
                "OpenAI API returned status " + response.getStatusCode())
                .withContext("status", response.getStatusCode().value())
                .withContext("body", response.getBody());
        }
        
        // Parse response
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode embeddingNode = root.path("data").get(0).path("embedding");
        
        if (embeddingNode == null || !embeddingNode.isArray()) {
            throw new EmbeddingException(ErrorCode.EMBEDDING_GENERATION_FAILED,
                "Invalid response from OpenAI API")
                .withContext("response", response.getBody());
        }
        
        // Convert to float array
        List<Float> embeddingList = new ArrayList<>();
        for (JsonNode value : embeddingNode) {
            embeddingList.add((float) value.asDouble());
        }
        
        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i);
        }
        
        log.debug("Generated embedding for text (length: {}, dimension: {})", 
            text.length(), embedding.length);
        
        return embedding;
    }
    
    @Override
    public int getDimension() {
        return DIMENSION;
    }
    
    @Override
    public String getProviderName() {
        return "openai";
    }
}

