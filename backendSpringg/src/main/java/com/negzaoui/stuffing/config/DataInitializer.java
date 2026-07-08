package com.negzaoui.stuffing.config;

import com.negzaoui.stuffing.entity.*;
import com.negzaoui.stuffing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * (collaborateurs, projets, assignments, skills, conges) pour remplir le dashboard.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               EmployeeProfileRepository employeeProfileRepository,
                               ProjectRepository projectRepository,
                               AssignmentRepository assignmentRepository,
                               LeaveRequestRepository leaveRequestRepository,
                               DepartementRepository departementRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {

            // ═══════════════════════════════════════════════════════
            //  1.  ADMIN par défaut
            // ═══════════════════════════════════════════════════════
            String adminEmail = "admin@soprahr.com";
            String adminPassword = "Admin1234!";

            var existingAdmin = userRepository.findByEmail(adminEmail);
            if (existingAdmin.isEmpty()) {
                var admin = User.builder()
                        .firstName("Admin")
                        .lastName("Stuffing")
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(Role.ADMIN)
                        .active(true)
                        .build();
                userRepository.save(admin);
                log.warn("Compte ADMIN cree: {} / {}", adminEmail, adminPassword);
            } else {
                var admin = existingAdmin.get();
                if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setActive(true);
                    userRepository.save(admin);
                    log.warn("Mot de passe ADMIN reinitialise: {} / {}", adminEmail, adminPassword);
                }
            }

            // ═══════════════════════════════════════════════════════
            //  2.  DELIVERY MANAGER de démo
            // ═══════════════════════════════════════════════════════
            String dmEmail = "karim.benali@soprahr.com";
            String dmPassword = "Manager1234!";

            User manager;
            var existingDm = userRepository.findByEmail(dmEmail);
            if (existingDm.isEmpty()) {
                manager = User.builder()
                        .firstName("Karim")
                        .lastName("Benali")
                        .email(dmEmail)
                        .password(passwordEncoder.encode(dmPassword))
                        .role(Role.DELIVERY_MANAGER)
                        .active(true)
                        .build();
                manager = userRepository.save(manager);
                log.warn("Compte DELIVERY_MANAGER cree: {} / {}", dmEmail, dmPassword);
            } else {
                manager = existingDm.get();
                if (!passwordEncoder.matches(dmPassword, manager.getPassword())) {
                    manager.setPassword(passwordEncoder.encode(dmPassword));
                    manager.setActive(true);
                    manager = userRepository.save(manager);
                    log.warn("Mot de passe DELIVERY_MANAGER reinitialise: {} / {}", dmEmail, dmPassword);
                }
            }

            // ─── Réinitialisation des mots de passe DELIVERY_MANAGER (autres) ───
                String dmDefaultPwd = "Delivery1234!";
            userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.DELIVERY_MANAGER && !u.getEmail().equals(dmEmail))
                    .forEach(u -> {
                        if (!passwordEncoder.matches(dmDefaultPwd, u.getPassword())) {
                            u.setPassword(passwordEncoder.encode(dmDefaultPwd));
                            u.setActive(true);
                            userRepository.save(u);
                            log.warn("Mot de passe DELIVERY_MANAGER reinitialise: {} / {}", u.getEmail(), dmDefaultPwd);
                        }
                    });

            // ─── Détection des mots de passe corrompus ───
            userRepository.findAll().forEach(user -> {
                String pwd = user.getPassword();
                if (pwd == null || !pwd.startsWith("$2a$") || pwd.length() != 60) {
                    log.error("⚠️  MOT DE PASSE CORROMPU détecté pour {} (email: {}). Hash: '{}' (len={})",
                            user.getFirstName(), user.getEmail(),
                            pwd != null ? pwd.substring(0, Math.min(pwd.length(), 10)) + "..." : "NULL",
                            pwd != null ? pwd.length() : 0);
                }
            });

            // ═══════════════════════════════════════════════════════
            //  3.  DONNÉES DE DÉMO (seulement si pas déjà présentes)
            // ═══════════════════════════════════════════════════════
            if (projectRepository.findByManagerId(manager.getId()).isEmpty()) {
                log.info("🔧 Insertion des donnees de demo pour le manager {} ...", dmEmail);
                seedDemoData(manager, userRepository, employeeProfileRepository,
                        projectRepository, assignmentRepository, leaveRequestRepository,
                        departementRepository, passwordEncoder);
                log.info("✅ Donnees de demo inserees avec succes !");
            } else {
                log.info("📦 Donnees de demo deja presentes pour le manager {}", dmEmail);
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════
    //  SEED : Collaborateurs + Projets + Assignments + Skills
    // ═══════════════════════════════════════════════════════════════

    private void seedDemoData(User manager,
                              UserRepository userRepository,
                              EmployeeProfileRepository employeeProfileRepository,
                              ProjectRepository projectRepository,
                              AssignmentRepository assignmentRepository,
                              LeaveRequestRepository leaveRequestRepository,
                              DepartementRepository departementRepository,
                              PasswordEncoder passwordEncoder) {

        String collabPassword = passwordEncoder.encode("Collab1234!");
        LocalDate now = LocalDate.now();

        // ─── Départements ────────────────────────────────────────
        Departement deptDSI= getOrCreateDepartement(departementRepository, "DSI");
        Departement deptData = getOrCreateDepartement(departementRepository, "Data");
        Departement deptQA = getOrCreateDepartement(departementRepository, "QA");
        Departement deptProdOps = getOrCreateDepartement(departementRepository, "ProdOps");
        Departement deptDesign = getOrCreateDepartement(departementRepository, "Design");

        // ─── 8 Collaborateurs ─────────────────────────────────

        User c1 = createUser(userRepository, "Yassine", "El Amrani", "yassine.elamrani@soprahr.com", collabPassword);
        User c2 = createUser(userRepository, "Fatima", "Zahra", "fatima.zahra@soprahr.com", collabPassword);
        User c3 = createUser(userRepository, "Omar", "Tazi", "omar.tazi@soprahr.com", collabPassword);
        User c4 = createUser(userRepository, "Sara", "Benmoussa", "sara.benmoussa@soprahr.com", collabPassword);
        User c5 = createUser(userRepository, "Amine", "Cherkaoui", "amine.cherkaoui@soprahr.com", collabPassword);
        User c6 = createUser(userRepository, "Nadia", "Idrissi", "nadia.idrissi@soprahr.com", collabPassword);
        User c7 = createUser(userRepository, "Mehdi", "Alaoui", "mehdi.alaoui@soprahr.com", collabPassword);
        User c8 = createUser(userRepository, "Leila", "Fassi", "leila.fassi@soprahr.com", collabPassword);

        // ─── Profils + Skills ─────────────────────────────────

        EmployeeProfile ep1 = createProfile(employeeProfileRepository, c1, "0661000001", deptDSI, List.of("Java", "Spring Boot", "Angular"));
        EmployeeProfile ep2 = createProfile(employeeProfileRepository, c2, "0661000002", deptDSI, List.of("React", "TypeScript", "Node.js"));
        EmployeeProfile ep3 = createProfile(employeeProfileRepository, c3, "0661000003", deptDSI, List.of("Python", "Django", "PostgreSQL"));
        EmployeeProfile ep4 = createProfile(employeeProfileRepository, c4, "0661000004", deptData, List.of("Python", "Machine Learning", "SQL"));
        EmployeeProfile ep5 = createProfile(employeeProfileRepository, c5, "0661000005", deptDSI, List.of("Java", "Spring Boot", "Docker", "Kubernetes"));
        EmployeeProfile ep6 = createProfile(employeeProfileRepository, c6, "0661000006", deptQA, List.of("Selenium", "JUnit", "Postman", "Cypress"));
        EmployeeProfile ep7 = createProfile(employeeProfileRepository, c7, "0661000007", deptProdOps, List.of("Docker", "Kubernetes", "Jenkins", "AWS"));
        EmployeeProfile ep8 = createProfile(employeeProfileRepository, c8, "0661000008", deptDesign, List.of("Figma", "Adobe XD", "CSS", "UX/UI"));

        // ─── 4 Projets ───────────────────────────────────────

        Project p1 = projectRepository.save(Project.builder()
                .name("Portail Client Banque Digitale")
                .description("Développement du portail client avec authentification forte et tableau de bord financier")
                .clientName("BankOfAfrica")
                .startDate(now.minusMonths(3))
                .endDate(now.plusMonths(6))
                .status(ProjectStatus.ACTIVE)
                .manager(manager)
                .technologies("Java,Spring Boot,Angular,PostgreSQL,Docker")
                .neededRessource("2 dev backend, 1 dev frontend, 1 QA")
                .build());

        Project p2 = projectRepository.save(Project.builder()
                .name("Plateforme E-commerce Marjane")
                .description("Refonte complète de la plateforme e-commerce avec moteur de recommandation IA")
                .clientName("Marjane Holding")
                .startDate(now.minusMonths(1))
                .endDate(now.plusMonths(9))
                .status(ProjectStatus.ACTIVE)
                .manager(manager)
                .technologies("React,Node.js,TypeScript,MongoDB,Python,Machine Learning")
                .neededRessource("2 dev fullstack, 1 data scientist, 1 designer")
                .build());

        Project p3 = projectRepository.save(Project.builder()
                .name("Migration Cloud OCP")
                .description("Migration de l'infrastructure on-premise vers AWS avec CI/CD")
                .clientName("OCP Group")
                .startDate(now.plusMonths(1))
                .endDate(now.plusMonths(7))
                .status(ProjectStatus.PLANNED)
                .manager(manager)
                .technologies("AWS,Docker,Kubernetes,Terraform,Jenkins")
                .neededRessource("1 architecte cloud, 2 devops, 1 dev backend")
                .build());

        Project p4 = projectRepository.save(Project.builder()
                .name("Application Mobile Inwi")
                .description("Développement de l'application mobile self-care pour les abonnés")
                .clientName("Inwi")
                .startDate(now.minusMonths(8))
                .endDate(now.minusMonths(1))
                .status(ProjectStatus.COMPLETED)
                .manager(manager)
                .technologies("React Native,Node.js,PostgreSQL")
                .neededRessource("2 dev mobile, 1 backend, 1 QA")
                .build());

        // ─── Assignments ──────────────────────────────────────

        // Projet 1 - Portail Banque (ACTIVE) → c1, c5, c6 staffés
        createAssignment(assignmentRepository, ep1, p1, "Développeur Backend Senior",
                now.minusMonths(3), now.plusMonths(6), AssignmentStatus.ACTIVE);
        createAssignment(assignmentRepository, ep5, p1, "Développeur Backend",
                now.minusMonths(2), now.plusMonths(6), AssignmentStatus.ACTIVE);
        createAssignment(assignmentRepository, ep6, p1, "Ingénieur QA",
                now.minusMonths(2), now.plusMonths(4), AssignmentStatus.ACTIVE);

        // Projet 2 - E-commerce (ACTIVE) → c2, c4, c8 staffés
        createAssignment(assignmentRepository, ep2, p2, "Développeur Frontend React",
                now.minusMonths(1), now.plusMonths(9), AssignmentStatus.ACTIVE);
        createAssignment(assignmentRepository, ep4, p2, "Data Scientist",
                now.minusMonths(1), now.plusMonths(9), AssignmentStatus.ACTIVE);
        createAssignment(assignmentRepository, ep8, p2, "UX/UI Designer",
                now.minusMonths(1), now.plusMonths(3), AssignmentStatus.ACTIVE);

        // Projet 4 - App Mobile Inwi (COMPLETED) → c2, c3 ont terminé
        createAssignment(assignmentRepository, ep3, p4, "Développeur Backend",
                now.minusMonths(8), now.minusMonths(1), AssignmentStatus.COMPLETED);

        // c7 a un assignment qui se termine bientôt → SOON_AVAILABLE
        createAssignment(assignmentRepository, ep7, p1, "Ingénieur DevOps",
                now.minusMonths(2), now.plusDays(15), AssignmentStatus.ACTIVE);

        // c3 est maintenant AVAILABLE (son dernier assignment est COMPLETED)
        // → il apparaitra comme "Disponible"

        // ─── Congés de démo ───────────────────────────────────

        // Yassine : congé payé approuvé (passé)
        leaveRequestRepository.save(LeaveRequest.builder()
                .user(c1).type(LeaveType.PAID_LEAVE)
                .startDate(now.minusMonths(2)).endDate(now.minusMonths(2).plusDays(4))
                .reason("Vacances familiales").status(LeaveStatus.APPROVED)
                .reviewedBy(manager).reviewedAt(LocalDateTime.now().minusMonths(2).minusDays(5))
                .build());

        // Yassine : congé payé approuvé (futur)
        leaveRequestRepository.save(LeaveRequest.builder()
                .user(c1).type(LeaveType.PAID_LEAVE)
                .startDate(now.plusMonths(1)).endDate(now.plusMonths(1).plusDays(4))
                .reason("Vacances d'ete").status(LeaveStatus.APPROVED)
                .reviewedBy(manager).reviewedAt(LocalDateTime.now().minusDays(3))
                .build());

        // Yassine : RTT en attente
        leaveRequestRepository.save(LeaveRequest.builder()
                .user(c1).type(LeaveType.RTT)
                .startDate(now.plusMonths(2)).endDate(now.plusMonths(2).plusDays(1))
                .reason("Personnel").status(LeaveStatus.PENDING)
                .build());

        // Fatima : congé payé approuvé
        leaveRequestRepository.save(LeaveRequest.builder()
                .user(c2).type(LeaveType.PAID_LEAVE)
                .startDate(now.plusWeeks(2)).endDate(now.plusWeeks(2).plusDays(6))
                .reason("Mariage").status(LeaveStatus.APPROVED)
                .reviewedBy(manager).reviewedAt(LocalDateTime.now().minusDays(1))
                .build());

        // Omar : arrêt maladie
        leaveRequestRepository.save(LeaveRequest.builder()
                .user(c3).type(LeaveType.SICK_LEAVE)
                .startDate(now.minusDays(10)).endDate(now.minusDays(8))
                .reason("Grippe").status(LeaveStatus.APPROVED)
                .reviewedBy(manager).reviewedAt(LocalDateTime.now().minusDays(11))
                .build());

        // Amine : RTT rejeté
        leaveRequestRepository.save(LeaveRequest.builder()
                .user(c5).type(LeaveType.RTT)
                .startDate(now.plusWeeks(1)).endDate(now.plusWeeks(1))
                .reason("Rendez-vous").status(LeaveStatus.REJECTED)
                .reviewedBy(manager).reviewedAt(LocalDateTime.now().minusDays(2))
                .build());

        // Nadia : congé payé en attente
        leaveRequestRepository.save(LeaveRequest.builder()
                .user(c6).type(LeaveType.PAID_LEAVE)
                .startDate(now.plusMonths(3)).endDate(now.plusMonths(3).plusDays(9))
                .reason("Voyage").status(LeaveStatus.PENDING)
                .build());

        log.info("   → 8 collaborateurs créés (mdp: Collab1234!)");
        log.info("   → 4 projets créés (2 ACTIVE, 1 PLANNED, 1 COMPLETED)");
        log.info("   → 8 assignments créés");
        log.info("   → 7 demandes de congé créées (3 APPROVED, 2 PENDING, 1 REJECTED, 1 SICK)");
    }

    // ─── Factory helpers ──────────────────────────────────────

    private User createUser(UserRepository repo, String firstName, String lastName,
                            String email, String encodedPassword) {
        return repo.findByEmail(email).orElseGet(() ->
                repo.save(User.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .password(encodedPassword)
                        .role(Role.COLLABORATEUR)
                        .active(true)
                        .build()));
    }

    private EmployeeProfile createProfile(EmployeeProfileRepository repo, User user,
                                           String phone, Departement departement,
                                           List<String> skillNames) {
        return repo.findByUserId(user.getId()).orElseGet(() -> {
            EmployeeProfile profile = EmployeeProfile.builder()
                    .user(user)
                    .phone(phone)
                    .departement(departement)
                    .build();
            // Ajouter les skills
            for (String name : skillNames) {
                Skill skill = Skill.builder()
                        .name(name)
                        .employeeProfile(profile)
                        .build();
                profile.getSkills().add(skill);
            }
            return repo.save(profile);
        });
    }

    private Departement getOrCreateDepartement(DepartementRepository repo, String name) {
        return repo.findByName(name).orElseGet(() ->
                repo.save(Departement.builder().name(name).build()));
    }

    private void createAssignment(AssignmentRepository repo, EmployeeProfile ep,
                                   Project project, String role,
                                   LocalDate start, LocalDate end,
                                   AssignmentStatus status) {
        repo.save(Assignment.builder()
                .employeeProfile(ep)
                .project(project)
                .projectName(project.getName())
                .clientName(project.getClientName())
                .roleName(role)
                .startDate(start)
                .endDate(end)
                .status(status)
                .build());
    }
}
