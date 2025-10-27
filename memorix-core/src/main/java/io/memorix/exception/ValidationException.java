package io.memorix.exception;

/**
 * Exception thrown when API request validation fails.
 * 
 * <p>This exception should result in 400 Bad Request response with descriptive
 * error message to help API clients understand what went wrong.
 * 
 * <p>Use this for:
 * <ul>
 *   <li>Empty or null required fields</li>
 *   <li>Invalid enum values</li>
 *   <li>Out of range numeric values</li>
 *   <li>Invalid format/pattern</li>
 * </ul>
 */
public class ValidationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final transient String field;
    private final transient Object rejectedValue;
    private final transient String expectedFormat;
    
    public ValidationException(String message) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
        this.expectedFormat = null;
    }
    
    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
        this.rejectedValue = null;
        this.expectedFormat = null;
    }
    
    public ValidationException(String field, Object rejectedValue, String message, String expectedFormat) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.expectedFormat = expectedFormat;
    }
    
    public String getField() {
        return field;
    }
    
    public Object getRejectedValue() {
        return rejectedValue;
    }
    
    public String getExpectedFormat() {
        return expectedFormat;
    }
}

