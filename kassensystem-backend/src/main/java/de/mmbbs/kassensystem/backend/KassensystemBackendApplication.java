package de.mmbbs.kassensystem.backend;

import de.mmbbs.kassensystem.backend.config.DatabasePathResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KassensystemBackendApplication {
    public static void main(String[] args) {
        System.setProperty(DatabasePathResolver.DB_PATH_PROPERTY, DatabasePathResolver.resolve().toString());
        if (System.getProperty("spring.datasource.url") == null && System.getenv("SPRING_DATASOURCE_URL") == null) {
            System.setProperty("spring.datasource.url", DatabasePathResolver.jdbcUrl());
        }
        SpringApplication.run(KassensystemBackendApplication.class, args);
    }
}
