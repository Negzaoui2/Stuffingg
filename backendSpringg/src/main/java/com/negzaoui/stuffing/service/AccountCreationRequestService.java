package com.negzaoui.stuffing.service;

import com.negzaoui.stuffing.dto.auth.AccountCreationRequestDto;
import com.negzaoui.stuffing.dto.auth.AccountRequestStatsDto;
import com.negzaoui.stuffing.entity.*;
import com.negzaoui.stuffing.repository.AccountCreationRequestRepository;
import com.negzaoui.stuffing.repository.DepartementRepository;
import com.negzaoui.stuffing.repository.EmployeeProfileRepository;
import com.negzaoui.stuffing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCreationRequestService {

    private final AccountCreationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final DepartementRepository departementRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final KeycloakAdminService keycloakAdminService;

    @Value("${app.frontend-url:http://localhost:4200/login}")
    private String frontendUrl;

    /**
     * Soumission d'une nouvelle demande.
     * Les doublons (même email avec une demande PENDING existante) sont autorisés :
     * chaque soumission est enregistrée pour que l'admin voie toutes les tentatives.
     */
    @Transactional
    public void submit(AccountCreationRequestDto dto) {
        Objects.requireNonNull(dto, "dto must not be null");

        var req = AccountCreationRequest.builder()
                .lastName(dto.getLastName())
                .firstName(dto.getFirstName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .company(dto.getCompany())
                .jobTitle(dto.getJobTitle())
                .message(dto.getMessage())
                .status(AccountRequestStatus.PENDING)
                .build();

        requestRepository.save(req);

        // Notifier tous les ADMIN qu'une nouvelle demande est arrivée
        notificationService.notifyAdmins(
                "Nouvelle demande de création de compte de " + dto.getFirstName() + " " + dto.getLastName() + " (" + dto.getEmail() + ")",
                "ACCOUNT_REQUEST"
        );

        // Notification basique (log/envoyer mail selon ta conf)
       // emailService.sendAccountRequestReceived(dto.getEmail());
    }

    /**
     * LIST : Toutes les demandes (triées DESC) ou filtrées par statut si fourni.
     * Utilisée par le contrôleur Variante A : GET /api/hr/account-requests?status=...
     */
    @Transactional(readOnly = true)
    public Page<AccountCreationRequest> list(AccountRequestStatus status, Pageable pageable) {
        if (status == null) {
            return requestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return requestRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
    }

    /**
     * STATS : compte exact des demandes par statut (utilise countByStatus côté repo).
     * Source unique de vérité pour les compteurs affichés dans le dashboard admin.
     */
    @Transactional(readOnly = true)
    public AccountRequestStatsDto stats() {
        long pending = requestRepository.countByStatus(AccountRequestStatus.PENDING);
        long approved = requestRepository.countByStatus(AccountRequestStatus.APPROVED);
        long rejected = requestRepository.countByStatus(AccountRequestStatus.REJECTED);
        return AccountRequestStatsDto.builder()
                .total(pending + approved + rejected)
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .build();
    }

    /**
     * APPROVE : Crée le user dans Keycloak + en BD locale, marque la demande APPROVED,
     * et envoie un email avec les credentials temporaires.
     */
    @Transactional
    public User approve(Long requestId, Role role, String temporaryPassword, Long managerId, Long departementId, Authentication processedBy) {
        var req = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        if (req.getStatus() != AccountRequestStatus.PENDING) {
            throw new IllegalStateException("Demande déjà traitée");
        }

        // ═══════════════════════════════════════════════════════
        // 0. Générer l'email PROFESSIONNEL (@soprahr.com)
        // ═══════════════════════════════════════════════════════
        String professionalEmail = generateProfessionalEmail(req.getFirstName(), req.getLastName());
        String personalEmail = req.getEmail(); // email personnel fourni dans la demande

        // Si le compte existe déjà localement (avec cet email pro), on valide sans recréer.
        if (userRepository.existsByEmail(professionalEmail)) {
            req.setStatus(AccountRequestStatus.APPROVED);
            req.setProcessedAt(Instant.now());
            req.setProcessedBy(processedBy != null ? processedBy.getName() : null);
            requestRepository.save(req);
            log.warn("⚠️  Le user {} existait déjà en BD locale. Demande marquée APPROVED sans recréation.", professionalEmail);
            return userRepository.findByEmail(professionalEmail).orElseThrow();
        }

        // Générer un mot de passe temporaire si non fourni
        String rawPassword = (temporaryPassword == null || temporaryPassword.isBlank())
                ? generateTempPassword()
                : temporaryPassword;

        // ═══════════════════════════════════════════════════════
        // 1. Créer (ou réutiliser) le user dans Keycloak avec l'email PRO
        // ═══════════════════════════════════════════════════════
        String keycloakId;
        try {
            String existingId = keycloakAdminService.findUserIdByEmail(professionalEmail);
            if (existingId != null) {
                log.warn("⚠️  Le user {} existe déjà dans Keycloak (id={}). Réinitialisation du mdp.", professionalEmail, existingId);
                keycloakAdminService.setUserPassword(existingId, rawPassword, true);
                keycloakAdminService.assignRealmRole(existingId, role.name());
                keycloakId = existingId;
            } else {
                keycloakId = keycloakAdminService.createUser(
                        professionalEmail, // ← email pro comme username/email Keycloak
                        req.getFirstName(),
                        req.getLastName(),
                        rawPassword,
                        role
                );
            }
        } catch (Exception e) {
            log.error("❌ Échec création user Keycloak pour {} : {}", professionalEmail, e.getMessage(), e);
            throw new RuntimeException("Impossible de créer le compte dans Keycloak : " + e.getMessage());
        }

        // ═══════════════════════════════════════════════════════
        // 2. Créer le user en BD locale avec l'email PRO
        // ═══════════════════════════════════════════════════════
        var user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(professionalEmail)        // ← email pro = identifiant
                .personalEmail(personalEmail)     // ← email personnel conservé
                .password(passwordEncoder.encode(rawPassword))
                .keycloakId(keycloakId)
                .role(role)
                .active(true)
                .build();

        userRepository.save(user);
        log.info("✅ User créé en BD locale : {} (email pro={}, email perso={}, keycloakId={})",
                user.getEmail(), professionalEmail, personalEmail, keycloakId);

        // ═══════════════════════════════════════════════════════
        // 2b. Créer un EmployeeProfile pour le nouveau user
        // ═══════════════════════════════════════════════════════
        User assignedManager = null;
        if (managerId != null && role == Role.COLLABORATEUR) {
            assignedManager = userRepository.findById(managerId)
                    .filter(m -> m.getRole() == Role.DELIVERY_MANAGER)
                    .orElse(null);
            if (assignedManager == null) {
                log.warn("⚠️  managerId={} introuvable ou n'est pas DELIVERY_MANAGER. Ignoré.", managerId);
            }
        }

        // Rattachement au département (valable pour COLLABORATEUR et DELIVERY_MANAGER)
        Departement assignedDepartement = null;
        if (departementId != null) {
            assignedDepartement = departementRepository.findById(departementId).orElse(null);
            if (assignedDepartement == null) {
                log.warn("⚠️  departementId={} introuvable. Ignoré.", departementId);
            }
        }

        EmployeeProfile profile = EmployeeProfile.builder()
                .user(user)
                .phone(req.getPhone())
                .departement(assignedDepartement)
                .manager(assignedManager)
                .build();
        employeeProfileRepository.save(profile);
        log.info("✅ EmployeeProfile créé pour {} (manager={}, departement={})", professionalEmail,
                assignedManager != null ? assignedManager.getEmail() : "aucun",
                assignedDepartement != null ? assignedDepartement.getName() : "aucun");

        // ═══════════════════════════════════════════════════════
        // 3. Mettre à jour la demande
        // ═══════════════════════════════════════════════════════
        req.setStatus(AccountRequestStatus.APPROVED);
        req.setProcessedAt(Instant.now());
        req.setProcessedBy(processedBy != null ? processedBy.getName() : null);
        requestRepository.save(req);

        // ═══════════════════════════════════════════════════════
        // 4. Envoyer email sur le mail PERSONNEL avec les identifiants PRO
        // ═══════════════════════════════════════════════════════
        try {
            emailService.sendAccountApprovedEmail(personalEmail, professionalEmail, rawPassword, frontendUrl);
        } catch (Exception e) {
            log.warn("Echec envoi email pour {} : {} (le compte a ete cree quand meme)", personalEmail, e.getMessage());
        }

        // ═══════════════════════════════════════════════════════
        // 5. Notification in-app
        // ═══════════════════════════════════════════════════════
        try {
            notificationService.createNotification(
                    "Bienvenue ! Votre compte a été créé. Votre identifiant : " + professionalEmail,
                    "ACCOUNT_APPROVED",
                    user
            );
        } catch (Exception e) {
            log.warn("Echec notification in-app pour {} : {}", user.getEmail(), e.getMessage());
        }

        return user;
    }

    /**
     * REJECT : Marque la demande REJECTED et conserve la trace (processedAt / processedBy).
     */
    @Transactional
    public void reject(Long requestId, String reason, Authentication processedBy) {
        var req = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        if (req.getStatus() != AccountRequestStatus.PENDING) {
            throw new IllegalStateException("Demande déjà traitée");
        }

        req.setStatus(AccountRequestStatus.REJECTED);
        req.setProcessedAt(Instant.now());
        req.setProcessedBy(processedBy != null ? processedBy.getName() : null);
        requestRepository.save(req);

        try {
            emailService.sendAccountRejectedEmail(req.getEmail(), reason);
        } catch (Exception e) {
            log.warn("Echec envoi email de rejet pour {} : {}", req.getEmail(), e.getMessage());
        }
    }

    // ---------------------
    // Utils
    // ---------------------

    private String generateTempPassword() {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#-";
        SecureRandom rnd = new SecureRandom();
        int len = 12;
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    /**
     * Génère un email professionnel @soprahr.com à partir du prénom et nom.
     * Ex: "Mohamed" + "Negzaoui" → "mohamed.negzaoui@soprahr.com"
     * Gère les accents, espaces, et doublons (ajoute un suffixe numérique si nécessaire).
     */
    private String generateProfessionalEmail(String firstName, String lastName) {
        String base = normalize(firstName) + "." + normalize(lastName) + "@soprahr.com";

        // Vérifier s'il n'y a pas déjà un user avec ce mail pro
        String candidate = base;
        int counter = 1;
        while (userRepository.existsByEmail(candidate)) {
            candidate = normalize(firstName) + "." + normalize(lastName) + counter + "@soprahr.com";
            counter++;
        }
        return candidate;
    }

    /**
     * Normalise un prénom/nom : minuscule, sans accents, sans espaces.
     */
    private String normalize(String input) {
        if (input == null) return "";
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replaceAll("[^a-zA-Z]", "")
                .toLowerCase();
    }
}