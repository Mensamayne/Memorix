package io.memorix.plugin;

import io.memorix.api.MemoryPlugin;
import io.memorix.exception.ErrorCode;
import io.memorix.exception.PluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for memory plugins.
 * 
 * <p>Manages plugin lifecycle:
 * <ul>
 *   <li>Register plugins</li>
 *   <li>Find plugins by type</li>
 *   <li>Validate plugins</li>
 *   <li>List all plugins</li>
 * </ul>
 * 
 * <p>Thread-safe implementation using ConcurrentHashMap.
 */
@Component
public class PluginRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(PluginRegistry.class);
    
    private final Map<String, MemoryPlugin> plugins = new ConcurrentHashMap<>();
    
    /**
     * Register a plugin.
     * 
     * @param plugin Plugin to register
     * @throws IllegalArgumentException if plugin is null
     * @throws PluginException if plugin type already registered
     */
    public void register(MemoryPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        
        String type = plugin.getType();
        if (type == null || type.trim().isEmpty()) {
            throw new PluginException(ErrorCode.PLUGIN_INVALID,
                "Plugin type cannot be null or empty")
                .withContext("pluginClass", plugin.getClass().getName());
        }
        
        // Validate plugin configuration
        validatePlugin(plugin);
        
        // Check for duplicates
        if (plugins.containsKey(type)) {
            throw new PluginException(ErrorCode.PLUGIN_INVALID,
                "Plugin type already registered: " + type)
                .withContext("existingPlugin", plugins.get(type).getClass().getName())
                .withContext("newPlugin", plugin.getClass().getName());
        }
        
        plugins.put(type, plugin);
        log.info("Registered plugin: {} ({})", type, plugin.getClass().getSimpleName());
    }
    
    /**
     * Find plugin by type.
     * 
     * @param type Plugin type identifier
     * @return Plugin if found
     */
    public Optional<MemoryPlugin> findByType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(plugins.get(type));
    }
    
    /**
     * Get plugin by type (throws if not found).
     * 
     * @param type Plugin type identifier
     * @return Plugin
     * @throws PluginException if plugin not found
     */
    public MemoryPlugin getByType(String type) {
        return findByType(type)
            .orElseThrow(() -> new PluginException(ErrorCode.PLUGIN_NOT_FOUND,
                "Plugin not found for type: " + type)
                .withContext("type", type)
                .withContext("availableTypes", plugins.keySet()));
    }
    
    /**
     * Unregister a plugin.
     * 
     * @param type Plugin type to unregister
     * @return true if plugin was removed, false if not found
     */
    public boolean unregister(String type) {
        if (type == null || type.trim().isEmpty()) {
            return false;
        }
        
        MemoryPlugin removed = plugins.remove(type);
        if (removed != null) {
            log.info("Unregistered plugin: {}", type);
            return true;
        }
        return false;
    }
    
    /**
     * Check if plugin type is registered.
     * 
     * @param type Plugin type
     * @return true if registered
     */
    public boolean isRegistered(String type) {
        return type != null && plugins.containsKey(type);
    }
    
    /**
     * Get all registered plugin types.
     * 
     * @return Set of registered types
     */
    public java.util.Set<String> getRegisteredTypes() {
        return java.util.Collections.unmodifiableSet(plugins.keySet());
    }
    
    /**
     * Get count of registered plugins.
     * 
     * @return Number of plugins
     */
    public int getPluginCount() {
        return plugins.size();
    }
    
    /**
     * Clear all registered plugins.
     * 
     * <p>Use with caution - mainly for testing.
     */
    public void clear() {
        plugins.clear();
        log.warn("Cleared all registered plugins");
    }
    
    /**
     * Validate plugin configuration.
     * 
     * @param plugin Plugin to validate
     * @throws PluginException if plugin is invalid
     */
    private void validatePlugin(MemoryPlugin plugin) {
        try {
            // Validate decay config
            var decayConfig = plugin.getDecayConfig();
            if (decayConfig == null) {
                throw new PluginException(ErrorCode.PLUGIN_INVALID,
                    "Plugin must provide DecayConfig")
                    .withContext("type", plugin.getType());
            }
            
            // Validate query limit
            var queryLimit = plugin.getDefaultQueryLimit();
            if (queryLimit == null) {
                throw new PluginException(ErrorCode.PLUGIN_INVALID,
                    "Plugin must provide default QueryLimit")
                    .withContext("type", plugin.getType());
            }
            
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginException(ErrorCode.PLUGIN_INVALID,
                "Plugin validation failed: " + e.getMessage(), e)
                .withContext("type", plugin.getType())
                .withContext("pluginClass", plugin.getClass().getName());
        }
    }
}

