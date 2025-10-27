package io.memorix.exception;

/**
 * Exception for plugin-related errors.
 * 
 * <p>Thrown when plugin operations fail, including:
 * <ul>
 *   <li>Plugin not found</li>
 *   <li>Invalid plugin configuration</li>
 *   <li>Plugin initialization failures</li>
 * </ul>
 */
public class PluginException extends MemorixException {
    
    private static final long serialVersionUID = 1L;
    
    public PluginException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public PluginException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

