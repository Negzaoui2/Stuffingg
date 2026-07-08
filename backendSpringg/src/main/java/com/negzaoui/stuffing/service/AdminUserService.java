package com.negzaoui.stuffing.service;

import com.negzaoui.stuffing.dto.admin.*;
import com.negzaoui.stuffing.entity.*;
import com.negzaoui.stuffing.repository.EmployeeProfileRepository;
import com.negzaoui.stuffing.repository.LeaveRequestRepository;
import com.negzaoui.stuffing.repository.NotificationRepository;
import com.negzaoui.stuffing.repository.PasswordResetTokenRepository;
import com.negzaoui.stuffing.repository.ProjectRepository;
import com.negzaoui.stuffing.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAdminService keycloakAdminService;
    private final NotificationRepository notificationRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ProjectRepository projectRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // ─── Liste paginee avec filtres ───

    @Transactional(readOnly = true)
    public UserPageResponse searchUsers(String search, String role, Boolean isActive, int page, int size) {

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtre recherche texte
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate firstNameLike = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNameLike = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(firstNameLike, lastNameLike, emailLike));
            }

            // Filtre par role
            if (role != null && !role.isBlank()) {
                try {
                    Role roleEnum = Role.valueOf(role);
                    predicates.add(cb.equal(root.get("role"), roleEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }

            // Filtre actif/inactif
            if (isActive != null) {
                predicates.add(cb.equal(root.get("active"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.findAll(spec, pageRequest);

        List<UserAdminDto> items = userPage.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return UserPageResponse.builder()
                .items(items)
                .total(userPage.getTotalElements())
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    // ─── Détail d'un user ───

    @Transactional(readOnly = true)
    public UserAdminDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable (id=" + id + ")"));
        return toDto(user);
    }

    // ─── Créer un user ───

    @Transactional
    public UserAdminDto createUser(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email déjà utilisé : " + req.getEmail());
        }

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .active(true)
                .build();

        user = userRepository.save(user);

        // Créer un profil employé vide associé
        EmployeeProfile profile = EmployeeProfile.builder()
                .user(user)
                .build();
        employeeProfileRepository.save(profile);

        user.setProfile(profile);
        return toDto(user);
    }

    // ─── Toggle actif/inactif ───

    @Transactional
    public ToggleStatusResponse toggleStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable (id=" + id + ")"));

        user.setActive(!user.isActive());
        userRepository.save(user);

        return ToggleStatusResponse.builder()
                .id(user.getId())
                .isActive(user.isActive())
                .build();
    }

    // ─── Supprimer un user ───

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable (id=" + id + ")"));

        // ═══════════════════════════════════════════════════════
        // 1. Nettoyer toutes les références vers ce user (sinon violation de FK)
        // ═══════════════════════════════════════════════════════

        // a) Notifications ciblant ce user
        notificationRepository.deleteByTargetUserId(id);

        // b) Demandes de congé du user + détacher les congés qu'il a révisés (en tant que manager)
        leaveRequestRepository.deleteByUserId(id);
        leaveRequestRepository.clearReviewedBy(id);

        // c) Tokens de réinitialisation de mot de passe
        passwordResetTokenRepository.deleteByUserId(id);

        // d) Détacher ce user en tant que manager des profils qu'il encadre
        employeeProfileRepository.clearManager(id);

        // e) Détacher ce user en tant que manager de ses projets
        projectRepository.clearManager(id);

        // ═══════════════════════════════════════════════════════
        // 2. Supprimer dans Keycloak
        // ═══════════════════════════════════════════════════════
        if (user.getKeycloakId() != null) {
            try {
                keycloakAdminService.deleteUser(user.getKeycloakId());
            } catch (Exception e) {
                log.warn("Impossible de supprimer le user dans Keycloak (id={}): {}", user.getKeycloakId(), e.getMessage());
            }
        } else {
            // Chercher par email dans Keycloak
            String kcId = keycloakAdminService.findUserIdByEmail(user.getEmail());
            if (kcId != null) {
                try {
                    keycloakAdminService.deleteUser(kcId);
                } catch (Exception e) {
                    log.warn("Impossible de supprimer le user dans Keycloak (email={}): {}", user.getEmail(), e.getMessage());
                }
            }
        }

        // ═══════════════════════════════════════════════════════
        // 3. Supprimer le user (cascade → EmployeeProfile → Skills + Assignments)
        // ═══════════════════════════════════════════════════════
        userRepository.delete(user);
        log.info("✅ Utilisateur supprimé : {} (id={})", user.getEmail(), id);
    }

    // ─── Reset password ───

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable (id=" + id + ")"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ─── Mapper : User → UserAdminDto ───

    private UserAdminDto toDto(User user) {
        EmployeeProfile profile = user.getProfile();
        if (profile == null) {
            profile = employeeProfileRepository.findByUserId(user.getId()).orElse(null);
        }

        List<String> skillNames = Collections.emptyList();
        List<AssignmentDto> assignmentDtos = Collections.emptyList();
        String phone = null;
        String department = null;

        if (profile != null) {
            phone = profile.getPhone();
            department = profile.getDepartment();

            if (profile.getSkills() != null) {
                skillNames = profile.getSkills().stream()
                        .map(Skill::getName)
                        .collect(Collectors.toList());
            }

            if (profile.getAssignments() != null) {
                assignmentDtos = profile.getAssignments().stream()
                        .map(this::toAssignmentDto)
                        .collect(Collectors.toList());
            }
        }

        return UserAdminDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DT_FMT) : null)
                .lastLogin(user.getLastLogin() != null ? user.getLastLogin().format(DT_FMT) : null)
                .phone(phone)
                .department(department)
                .skills(skillNames)
                .assignments(assignmentDtos)
                .build();
    }

    private AssignmentDto toAssignmentDto(Assignment a) {
        return AssignmentDto.builder()
                .id(a.getId())
                .projectName(a.getProjectName())
                .clientName(a.getClientName())
                .roleName(a.getRoleName())
                .startDate(a.getStartDate() != null ? a.getStartDate().format(D_FMT) : null)
                .endDate(a.getEndDate() != null ? a.getEndDate().format(D_FMT) : null)
                .status(a.getStatus() != null ? a.getStatus().name() : null)
                .build();
    }
}

