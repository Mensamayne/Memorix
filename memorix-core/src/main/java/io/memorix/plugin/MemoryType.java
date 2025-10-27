package io.memorix.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark memory type plugins.
 * 
 * <p>Used for automatic plugin discovery and registration.
 * 
 * <p>Example:
 * <pre>{@code
 * @Component
 * @MemoryType("USER_PREFERENCE")
 * public class UserPreferencePlugin implements MemoryPlugin {
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MemoryType {
    
    /**
     * Type identifier for this memory plugin.
     * 
     * @return Type identifier (e.g., "USER_PREFERENCE", "DOCUMENT", "CONVERSATION")
     */
    String value();
}

