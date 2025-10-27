package io.memorix.exception;

/**
 * Exception for embedding-related errors.
 * 
 * <p>Thrown when embedding operations fail, including:
 * <ul>
 *   <li>Embedding generation failures</li>
 *   <li>Provider not found</li>
 *   <li>Dimension mismatches</li>
 * </ul>
 */
public class EmbeddingException extends MemorixException {
    
    private static final long serialVersionUID = 1L;
    
    public EmbeddingException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public EmbeddingException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

