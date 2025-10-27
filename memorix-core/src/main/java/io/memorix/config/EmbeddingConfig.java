package io.memorix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for embedding providers.
 * 
 * <p>Configured via application.yml:
 * <pre>{@code
 * memorix:
 *   embedding:
 *     provider: openai
 *     openai:
 *       api-key: ${OPENAI_API_KEY}
 *       model: text-embedding-3-small
 * }</pre>
 */
@Configuration
public class EmbeddingConfig {
    
    @Value("${memorix.embedding.provider:openai}")
    private String provider;
    
    @Value("${memorix.embedding.openai.api-key:}")
    private String openaiApiKey;
    
    @Value("${memorix.embedding.openai.model:text-embedding-3-small}")
    private String openaiModel;
    
    @Value("${memorix.embedding.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;
    
    @Value("${memorix.embedding.openai.timeout:30000}")
    private int openaiTimeout;
    
    @Value("${memorix.embedding.openai.max-retries:3}")
    private int openaiMaxRetries;
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public OpenAIConfig getOpenai() {
        OpenAIConfig config = new OpenAIConfig();
        config.setApiKey(openaiApiKey);
        config.setModel(openaiModel);
        config.setBaseUrl(openaiBaseUrl);
        config.setTimeout(openaiTimeout);
        config.setMaxRetries(openaiMaxRetries);
        return config;
    }
    
    public void setOpenai(OpenAIConfig openai) {
        if (openai != null) {
            this.openaiApiKey = openai.getApiKey();
            this.openaiModel = openai.getModel();
            this.openaiBaseUrl = openai.getBaseUrl();
            this.openaiTimeout = openai.getTimeout();
            this.openaiMaxRetries = openai.getMaxRetries();
        }
    }
    
    /**
     * OpenAI-specific configuration.
     */
    public static class OpenAIConfig {
        private String apiKey;
        private String model;
        private String baseUrl;
        private int timeout;
        private int maxRetries;
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
        
        public int getMaxRetries() {
            return maxRetries;
        }
        
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }
}

