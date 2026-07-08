package com.negzaoui.stuffing.security;

import com.negzaoui.stuffing.entity.EmployeeProfile;
import com.negzaoui.stuffing.entity.Role;
import com.negzaoui.stuffing.entity.User;
import com.negzaoui.stuffing.repository.EmployeeProfileRepository;
import com.negzaoui.stuffing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakHelper {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    public Jwt getJwt(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Aucun token JWT fourni. Authentifiez-vous via Keycloak et envoyez le token dans l'en-tête Authorization: Bearer <token>.");
        }
        if (!(authentication.getPrincipal() instanceof Jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token invalide ou non-JWT.");
        }
        return (Jwt) authentication.getPrincipal();
    }

    public String getEmail(Authentication authentication) {
        Jwt jwt = getJwt(authentication);
        String email = jwt.getClaimAsString("email");
        if (email != null) return email;
        return jwt.getClaimAsString("preferred_username");
    }

    @Transactional
    public User getCurrentUser(Authentication authentication) {
        Jwt jwt = getJwt(authentication);
        String email = getEmail(authentication);

        return userRepository.findByEmail(email)
                .orElseGet(() -> autoProvisionUser(jwt, email));
    }

    @Transactional
    public Long getCurrentUserId(Authentication authentication) {
        return getCurrentUser(authentication).getId();
    }

    // =====================================================================
    //  AUTO-PROVISIONING : Creation automatique du user en BDD
    // =====================================================================

    private User autoProvisionUser(Jwt jwt, String email) {
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        Role role = extractRole(jwt);

        if (firstName == null || firstName.isBlank()) firstName = "Nouveau";
        if (lastName == null || lastName.isBlank()) lastName = "Utilisateur";

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password("KEYCLOAK_MANAGED")
                .role(role)
                .active(true)
                .build();

        user = userRepository.save(user);

        EmployeeProfile profile = EmployeeProfile.builder()
                .user(user)
                .build();
        employeeProfileRepository.save(profile);

        user.setProfile(profile);

        log.info("AUTO-PROVISIONING : Utilisateur '{}' ({}) cree en BDD locale avec le role {}",
                email, firstName + " " + lastName, role.name());

        return user;
    }

    @SuppressWarnings("unchecked")
    private Role extractRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || realmAccess.get("roles") == null) {
            return Role.COLLABORATEUR;
        }

        Collection<String> roles = (Collection<String>) realmAccess.get("roles");

        if (roles.contains("ADMIN")) return Role.ADMIN;
        if (roles.contains("DELIVERY_MANAGER")) return Role.DELIVERY_MANAGER;
        if (roles.contains("COLLABORATEUR")) return Role.COLLABORATEUR;

        return Role.COLLABORATEUR;
    }
}
