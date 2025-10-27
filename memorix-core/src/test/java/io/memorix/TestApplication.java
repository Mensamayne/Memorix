package io.memorix;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test configuration for Spring Boot tests.
 */
@SpringBootApplication
@ComponentScan(basePackages = "io.memorix")
public class TestApplication {
    // Spring Boot auto-configuration handles DataSource and JdbcTemplate
}

