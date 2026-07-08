package com.negzaoui.stuffing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration applicative.
 * L'authentification est maintenant déléguée à Keycloak (OAuth2 Resource Server).
 * On garde le PasswordEncoder pour les opérations internes (DataInitializer, etc.).
 */
@Configuration
public class ApplicationConfig {


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
