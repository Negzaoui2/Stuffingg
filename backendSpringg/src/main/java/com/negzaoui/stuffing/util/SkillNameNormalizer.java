package com.negzaoui.stuffing.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalise les noms de compétences pour éviter les doublons sémantiques
 * dans l'autocomplete (ex: "reactjs", "React.js", "react js" → "React").
 *
 * Stratégie :
 *  1. trim + réduction des espaces multiples
 *  2. application d'un dictionnaire d'alias connus (clé en forme normalisée)
 *  3. sinon, Title Case (1ʳᵉ lettre de chaque mot en majuscule)
 */
public final class SkillNameNormalizer {

    private SkillNameNormalizer() {}

    private static final Map<String, String> ALIASES = new HashMap<>();
    static {
        // Frontend frameworks / libs
        put("React",       "react", "reactjs", "react.js", "react js");
        put("Vue.js",      "vue", "vuejs", "vue.js", "vue js");
        put("Angular",     "angular", "angularjs", "angular.js", "angular js");
        put("Next.js",     "next", "nextjs", "next.js", "next js");
        put("Nuxt.js",     "nuxt", "nuxtjs", "nuxt.js");
        put("Svelte",      "svelte", "sveltejs");

        // Backend / langages
        put("Node.js",     "node", "nodejs", "node.js", "node js");
        put("NestJS",      "nest", "nestjs", "nest.js");
        put("Spring Boot", "spring boot", "springboot", "spring-boot", "spring");
        put("JavaScript",  "js", "javascript", "java script");
        put("TypeScript",  "ts", "typescript", "type script");
        put("Java",        "java");
        put("Python",      "python", "py");
        put("C#",          "c#", "csharp", "c sharp");
        put("C++",         "c++", "cpp", "cplusplus");
        put("PHP",         "php");
        put("Go",          "go", "golang");
        put("Rust",        "rust");
        put("Kotlin",      "kotlin");
        put("Ruby",        "ruby");

        // Databases
        put("PostgreSQL",  "postgres", "postgresql", "psql");
        put("MySQL",       "mysql");
        put("MongoDB",     "mongo", "mongodb");
        put("Redis",       "redis");
        put("Oracle",      "oracle", "oracle db");
        put("SQL Server",  "sql server", "sqlserver", "mssql");

        // DevOps / Cloud
        put("Docker",      "docker");
        put("Kubernetes",  "k8s", "kubernetes", "kube");
        put("AWS",         "aws", "amazon web services");
        put("Azure",       "azure", "microsoft azure");
        put("GCP",         "gcp", "google cloud", "google cloud platform");
        put("CI/CD",       "ci/cd", "cicd", "ci cd");
        put("Jenkins",     "jenkins");
        put("Git",         "git");

        // Web
        put("HTML",        "html", "html5");
        put("CSS",         "css", "css3");
        put("SASS",        "sass", "scss");
        put("Tailwind CSS","tailwind", "tailwindcss", "tailwind css");
        put("Bootstrap",   "bootstrap");
    }

    private static void put(String canonical, String... aliases) {
        for (String a : aliases) {
            ALIASES.put(key(a), canonical);
        }
        ALIASES.put(key(canonical), canonical);
    }

    /** Clé de comparaison : lowercase, sans accent, espaces simples. */
    private static String key(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .replaceAll("\\s+", " ");
        return n;
    }

    /**
     * Normalise un nom de compétence : trim, alias connus, ou Title Case par défaut.
     * Retourne null si l'entrée est null ou vide après trim.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) return null;

        String aliased = ALIASES.get(key(trimmed));
        if (aliased != null) return aliased;

        return toTitleCase(trimmed);
    }

    private static String toTitleCase(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        boolean nextUpper = true;
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-' || c == '/') {
                sb.append(c);
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
