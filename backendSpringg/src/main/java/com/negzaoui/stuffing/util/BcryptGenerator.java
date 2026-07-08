package com.negzaoui.stuffing.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilitaire pour générer ET vérifier un hash BCrypt.
 *
 * Ce fichier n'est PAS un bean Spring. C'est un main() à exécuter manuellement.
 * Usage : Run → copier EXACTEMENT le hash (60 chars, commence par $2a$)
 */
public class BcryptGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "admin11";
        String hash = encoder.encode(rawPassword);

        System.out.println("========================================");
        System.out.println("Mot de passe brut : " + rawPassword);
        System.out.println("Hash BCrypt       : " + hash);
        System.out.println("Longueur du hash  : " + hash.length() + " (doit être 60)");
        System.out.println("Commence par $2a$ : " + hash.startsWith("$2a$"));
        System.out.println("========================================");

        // Vérification immédiate
        boolean matches = encoder.matches(rawPassword, hash);
        System.out.println("Vérification (matches) : " + matches + " (doit être true)");

        // Simule ce qui se passe si on copie mal le hash
        String testHash = hash.trim();
        System.out.println("Vérif après trim  : " + encoder.matches(rawPassword, testHash));
    }
}

