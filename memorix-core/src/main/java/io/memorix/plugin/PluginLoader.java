package io.memorix.plugin;

import io.memorix.api.MemoryPlugin;
import io.memorix.exception.PluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Automatic plugin loader.
 * 
 * <p>Discovers and registers plugins marked with @MemoryType annotation.
 * Uses Spring ApplicationContext to find all MemoryPlugin beans.
 */
@Component
public class PluginLoader implements InitializingBean {
    
    private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);
    
    private final ApplicationContext context;
    private final PluginRegistry registry;
    
    @Autowired
    public PluginLoader(ApplicationContext context, PluginRegistry registry) {
        this.context = context;
        this.registry = registry;
    }
    
    /**
     * Load and register all plugins on application startup.
     */
    @Override
    public void afterPropertiesSet() {
        loadPlugins();
    }
    
    /**
     * Load all plugins from application context.
     */
    public void loadPlugins() {
        log.info("Loading memory plugins...");
        
        Map<String, MemoryPlugin> pluginBeans = context.getBeansOfType(MemoryPlugin.class);
        
        if (pluginBeans.isEmpty()) {
            log.warn("No memory plugins found in application context");
            return;
        }
        
        int loaded = 0;
        for (Map.Entry<String, MemoryPlugin> entry : pluginBeans.entrySet()) {
            MemoryPlugin plugin = entry.getValue();
            
            try {
                registry.register(plugin);
                loaded++;
            } catch (PluginException e) {
                log.error("Failed to register plugin: {} - {}", 
                    entry.getKey(), e.getMessage());
                throw e;
            }
        }
        
        log.info("Loaded {} memory plugin(s): {}", loaded, registry.getRegisteredTypes());
    }
    
    /**
     * Reload all plugins.
     * 
     * <p>Clears registry and re-registers all plugins.
     * Use with caution.
     */
    public void reload() {
        log.info("Reloading all plugins...");
        registry.clear();
        loadPlugins();
    }
}

