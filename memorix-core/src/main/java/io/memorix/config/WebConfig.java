package io.memorix.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for serving static resources.
 * 
 * <p>Configures playground static files to be served at /playground/ path.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/playground/**")
                .addResourceLocations("classpath:/static/playground/");
    }
}
