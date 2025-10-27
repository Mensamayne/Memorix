package io.memorix.exception;

/**
 * Error codes for Memorix exceptions.
 * 
 * <p>Organized by category for easy identification:
 * <ul>
 *   <li>1xxx: Storage errors</li>
 *   <li>2xxx: Plugin errors</li>
 *   <li>3xxx: Embedding errors</li>
 *   <li>4xxx: Query errors</li>
 *   <li>5xxx: Lifecycle errors</li>
 *   <li>9xxx: General errors</li>
 * </ul>
 */
public enum ErrorCode {
    
    // Storage errors (1xxx)
    CONNECTION_FAILED(1001, "Failed to connect to database"),
    QUERY_FAILED(1002, "Database query failed"),
    TRANSACTION_FAILED(1003, "Transaction failed"),
    SCHEMA_INVALID(1004, "Database schema is invalid"),
    SAVE_FAILED(1005, "Failed to save object"),
    DELETE_FAILED(1006, "Failed to delete object"),
    UPDATE_FAILED(1007, "Failed to update object"),
    DUPLICATE_MEMORY(1008, "Duplicate memory detected"),
    
    // Plugin errors (2xxx)
    PLUGIN_NOT_FOUND(2001, "Plugin not found"),
    PLUGIN_INVALID(2002, "Plugin configuration is invalid"),
    PLUGIN_INITIALIZATION_FAILED(2003, "Failed to initialize plugin"),
    
    // Embedding errors (3xxx)
    EMBEDDING_GENERATION_FAILED(3001, "Failed to generate embedding"),
    EMBEDDING_PROVIDER_NOT_FOUND(3002, "Embedding provider not found"),
    EMBEDDING_DIMENSION_MISMATCH(3003, "Embedding dimension mismatch"),
    
    // Query errors (4xxx)
    QUERY_INVALID(4001, "Query is invalid"),
    LIMIT_INVALID(4002, "Query limit is invalid"),
    SIMILARITY_THRESHOLD_INVALID(4003, "Similarity threshold is invalid"),
    
    // Lifecycle errors (5xxx)
    DECAY_CALCULATION_FAILED(5001, "Failed to calculate decay"),
    CLEANUP_FAILED(5002, "Failed to cleanup expired objects"),
    
    // General errors (9xxx)
    INVALID_ARGUMENT(9001, "Invalid argument"),
    UNSUPPORTED_OPERATION(9002, "Operation not supported"),
    CONFIGURATION_ERROR(9003, "Configuration error"),
    INTERNAL_ERROR(9999, "Internal error");
    
    private final int code;
    private final String description;
    
    ErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}

