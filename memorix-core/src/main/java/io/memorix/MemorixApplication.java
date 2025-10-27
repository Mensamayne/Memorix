package io.memorix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Memorix Spring Boot Application.
 * 
 * <p>Starts embedded server with REST API and playground.
 * 
 * <p>Access playground at: http://localhost:8080/playground/
 */
@SpringBootApplication
public class MemorixApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MemorixApplication.class, args);
    }
}

