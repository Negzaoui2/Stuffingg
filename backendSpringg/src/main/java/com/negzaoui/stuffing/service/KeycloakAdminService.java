package com.negzaoui.stuffing.service;

import com.negzaoui.stuffing.entity.Role;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Service pour gérer les utilisateurs dans Keycloak via l'API Admin.
 * - Création d'un user
 * - Affectation d'un rôle realm
 * - Définition d'un mot de passe (temporaire ou non)
 * - Activation / désactivation
 * - Suppression
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Crée un utilisateur dans Keycloak avec un mot de passe temporaire et lui assigne un rôle realm.
     *
     * @param email             email = username
     * @param firstName         prénom
     * @param lastName          nom
     * @param temporaryPassword mot de passe temporaire (l'utilisateur devra le changer au 1er login)
     * @param role              rôle métier (ADMIN / DELIVERY_MANAGER / COLLABORATEUR)
     * @return l'ID Keycloak (UUID) du user créé
     */
    public String createUser(String email, String firstName, String lastName,
                              String temporaryPassword, Role role) {

        RealmResource realmResource = keycloakAdminClient.realm(realm);
        UsersResource usersResource = realmResource.users();

        // 1. Construire la représentation du user
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(true);

        // 2. Créer le user
        Response response = usersResource.create(user);

        if (response.getStatus() != 201) {
            String errorMsg = "Échec création user Keycloak. Status=" + response.getStatus()
                    + ", Body=" + response.readEntity(String.class);
            log.error(errorMsg);
            response.close();
            throw new RuntimeException(errorMsg);
        }

        // 3. Récupérer l'ID Keycloak depuis le header Location
        String userId = extractUserIdFromLocation(response.getLocation());
        response.close();
        log.info("✅ User créé dans Keycloak : email={}, id={}", email, userId);

        // 4. Définir le mot de passe (TEMPORAIRE = forcera le changement au premier login)
        setUserPassword(userId, temporaryPassword, true);

        // 5. Assigner le rôle realm
        assignRealmRole(userId, role.name());

        return userId;
    }

    /**
     * Définit (ou réinitialise) le mot de passe d'un user Keycloak.
     *
     * @param userId    ID Keycloak (UUID)
     * @param password  nouveau mot de passe
     * @param temporary true = forcer le changement au prochain login
     */
    public void setUserPassword(String userId, String password, boolean temporary) {
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        cred.setTemporary(temporary);

        keycloakAdminClient.realm(realm)
                .users()
                .get(userId)
                .resetPassword(cred);

        log.info("🔐 Mot de passe défini pour userId={} (temporary={})", userId, temporary);
    }

    /**
     * Assigne un rôle realm à un utilisateur.
     */
    public void assignRealmRole(String userId, String roleName) {
        RealmResource realmResource = keycloakAdminClient.realm(realm);

        try {
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            realmResource.users().get(userId)
                    .roles().realmLevel()
                    .add(Collections.singletonList(role));

            log.info("🎭 Rôle '{}' assigné au userId={}", roleName, userId);
        } catch (Exception e) {
            log.error("❌ Impossible d'assigner le rôle '{}' au userId={} : {}",
                    roleName, userId, e.getMessage());
            throw new RuntimeException("Le rôle '" + roleName + "' n'existe pas dans Keycloak. " +
                    "Créez-le d'abord dans le realm '" + realm + "'.");
        }
    }

    /**
     * Active ou désactive un user dans Keycloak.
     */
    public void setUserEnabled(String userId, boolean enabled) {
        UserResource userResource = keycloakAdminClient.realm(realm).users().get(userId);
        UserRepresentation rep = userResource.toRepresentation();
        rep.setEnabled(enabled);
        userResource.update(rep);
        log.info("🔄 User Keycloak {} : enabled={}", userId, enabled);
    }

    /**
     * Supprime un user de Keycloak.
     */
    public void deleteUser(String userId) {
        keycloakAdminClient.realm(realm).users().get(userId).remove();
        log.info("🗑️  User Keycloak supprimé : {}", userId);
    }

    /**
     * Recherche un user par email. Retourne l'ID Keycloak s'il existe, null sinon.
     */
    public String findUserIdByEmail(String email) {
        List<UserRepresentation> users = keycloakAdminClient.realm(realm)
                .users()
                .searchByEmail(email, true);

        if (users == null || users.isEmpty()) {
            return null;
        }
        return users.get(0).getId();
    }

    // ─── Helper ───────────────────────────────────────────

    private String extractUserIdFromLocation(URI location) {
        if (location == null) return null;
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}

