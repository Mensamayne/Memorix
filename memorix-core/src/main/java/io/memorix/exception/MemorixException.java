package io.memorix.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all Memorix errors.
 * 
 * <p>All custom exceptions extend this class and include:
 * <ul>
 *   <li>Error code for categorization</li>
 *   <li>Context map for debugging information</li>
 *   <li>Clear error messages</li>
 * </ul>
 */
public class MemorixException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final ErrorCode errorCode;
    private final transient Map<String, Object> context;
    
    public MemorixException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    public MemorixException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    /**
     * Add context information for debugging.
     * 
     * @param key Context key
     * @param value Context value
     * @return This exception (for chaining)
     */
    public MemorixException withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName())
          .append(" [")
          .append(errorCode)
          .append("]: ")
          .append(getMessage());
        
        if (!context.isEmpty()) {
            sb.append(" | Context: ").append(context);
        }
        
        return sb.toString();
    }
}

