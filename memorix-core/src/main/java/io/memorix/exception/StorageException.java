package io.memorix.exception;

/**
 * Exception for storage-related errors.
 * 
 * <p>Thrown when database operations fail, including:
 * <ul>
 *   <li>Connection failures</li>
 *   <li>Query execution errors</li>
 *   <li>Transaction failures</li>
 *   <li>Schema issues</li>
 * </ul>
 */
public class StorageException extends MemorixException {
    
    private static final long serialVersionUID = 1L;
    
    public StorageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public StorageException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

