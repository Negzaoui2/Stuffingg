package com.negzaoui.stuffing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Entité User.
 * L'authentification est déléguée à Keycloak.
 * Cette entité sert uniquement de référence locale (profil, assignments, etc.).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(hidden = true)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Email personnel du collaborateur (fourni lors de la demande d'accès).
     * Sert à envoyer le mail de bienvenue avec les identifiants pro.
     */
    @Column
    private String personalEmail;

    @JsonIgnore
    @Column(nullable = true)
    private String password;

    /**
     * ID de l'utilisateur dans Keycloak (UUID retourné lors de la création).
     * Permet de retrouver / modifier / supprimer le user dans Keycloak.
     */
    @Column(unique = true)
    private String keycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.COLLABORATEUR;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "timestamp default now()")
    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private EmployeeProfile profile;
}
