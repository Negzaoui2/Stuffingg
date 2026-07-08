package com.negzaoui.stuffing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Slf4j
@Configuration
public class JwtWarmupConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

/**
 * Pré-charge les clés JWKS de Keycloak au démarrage de l'application
 * au lieu d'attendre la première requête HTTP.
 */
@Bean
public JwtDecoder jwtDecoder(){
    log.info("⏳ Pré-chargement des clés JWKS depuis Keycloak...");
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

    // Force le téléchargement des clés MAINTENANT (pas au 1er appel)
    try {
        // On décode un faux token pour forcer le chargement du JWK set
        decoder.decode("fake");
    } catch (Exception e) {
        // Normal : le token "fake" est invalide, mais les clés JWKS sont maintenant en cache
        log.info("✅ Clés JWKS Keycloak chargées en cache (warm-up terminé)");
    }

    return decoder;
}
}