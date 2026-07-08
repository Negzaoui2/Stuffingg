package com.negzaoui.stuffing.service;

import com.negzaoui.stuffing.dto.manager.*;
import com.negzaoui.stuffing.entity.*;
import com.negzaoui.stuffing.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private final ProjectRepository projectRepository;
    private final AssignmentRepository assignmentRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int SOON_AVAILABLE_DAYS = 30;

    // ═══════════════════════════════════════════════════════════
    //  1.  GET /api/manager/dashboard
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ManagerDashboardDto getDashboard(Long managerId) {
        List<Project> projects = projectRepository.findByManagerId(managerId);
        Set<EmployeeProfile> teamProfiles = collectTeamProfiles(projects, managerId);

        int totalCollaborators = teamProfiles.size();
        int activeProjects = (int) projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE).count();

        // Availability
        LocalDate now = LocalDate.now();
        LocalDate soonLimit = now.plusDays(SOON_AVAILABLE_DAYS);
        int staffed = 0, available = 0, soonAvailable = 0;
        for (EmployeeProfile ep : teamProfiles) {
            String avail = computeAvailability(ep, now, soonLimit);
            switch (avail) {
                case "STAFFED" -> staffed++;
                case "AVAILABLE" -> available++;
                case "SOON_AVAILABLE" -> soonAvailable++;
            }
        }
        double occupancyRate = totalCollaborators == 0 ? 0.0
                : (double) staffed / totalCollaborators;

        // Skill distribution
        Map<String, Integer> skillMap = new LinkedHashMap<>();
        for (EmployeeProfile ep : teamProfiles) {
            for (Skill s : ep.getSkills()) {
                skillMap.merge(s.getName(), 1, Integer::sum);
            }
        }
        List<SkillCountDto> skillDistribution = skillMap.entrySet().stream()
                .map(e -> SkillCountDto.builder().name(e.getKey()).count(e.getValue()).build())
                .sorted(Comparator.comparingInt(SkillCountDto::getCount).reversed())
                .collect(Collectors.toList());

        // Project status distribution
        Map<String, Integer> statusMap = new LinkedHashMap<>();
        for (Project p : projects) {
            statusMap.merge(p.getStatus().name(), 1, Integer::sum);
        }
        List<ProjectStatusCountDto> projectStatusDistribution = statusMap.entrySet().stream()
                .map(e -> ProjectStatusCountDto.builder().status(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());

        // Recent assignments (last 10)
        List<Assignment> allAssignments = assignmentRepository.findRecentByManagerId(managerId);
        List<RecentAssignmentDto> recentAssignments = allAssignments.stream()
                .limit(10)
                .map(a -> {
                    String type = determineAssignmentType(a, now);
                    String collabName = getCollaboratorName(a);
                    String date = a.getStartDate() != null ? a.getStartDate().format(D_FMT) : null;
                    return RecentAssignmentDto.builder()
                            .collaboratorName(collabName)
                            .projectName(a.getProjectName())
                            .date(date)
                            .type(type)
                            .build();
                })
                .collect(Collectors.toList());

        return ManagerDashboardDto.builder()
                .totalCollaborators(totalCollaborators)
                .activeProjects(activeProjects)
                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                .availableCollaborators(available)
                .soonAvailableCollaborators(soonAvailable)
                .skillDistribution(skillDistribution)
                .projectStatusDistribution(projectStatusDistribution)
                .recentAssignments(recentAssignments)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  2.  GET /api/manager/team
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponseDto<CollaboratorDto> getTeam(Long managerId, String search,
                                                     String skill, String availability,
                                                     int page, int size) {
        List<Project> projects = projectRepository.findByManagerId(managerId);
        Set<EmployeeProfile> teamProfiles = collectTeamProfiles(projects, managerId);

        LocalDate now = LocalDate.now();
        LocalDate soonLimit = now.plusDays(SOON_AVAILABLE_DAYS);

        // Apply filters
        List<CollaboratorDto> all = teamProfiles.stream()
                .map(ep -> toCollaboratorDto(ep, now, soonLimit))
                .filter(dto -> matchSearch(dto, search))
                .filter(dto -> matchSkill(dto, skill))
                .filter(dto -> matchAvailability(dto, availability))
                .sorted(Comparator.comparing(CollaboratorDto::getLastName))
                .collect(Collectors.toList());

        // Paginate
        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<CollaboratorDto> items = all.subList(fromIndex, toIndex);

        return PageResponseDto.<CollaboratorDto>builder()
                .items(items)
                .total(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  3.  GET /api/manager/team/{id}
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CollaboratorDto getCollaboratorDetail(Long managerId, Long userId) {
        // Verify the collaborator belongs to one of the manager's projects
        List<Project> projects = projectRepository.findByManagerId(managerId);
        Set<EmployeeProfile> teamProfiles = collectTeamProfiles(projects, managerId);

        EmployeeProfile ep = teamProfiles.stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Collaborateur introuvable ou non dans votre equipe (id=" + userId + ")"));

        LocalDate now = LocalDate.now();
        return toCollaboratorDto(ep, now, now.plusDays(SOON_AVAILABLE_DAYS));
    }

    // ═══════════════════════════════════════════════════════════
    //  4.  GET /api/manager/projects
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponseDto<ProjectDto> getProjects(Long managerId, String search,
                                                    String status, int page, int size) {
        Specification<Project> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("manager").get("id"), managerId));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate clientLike = cb.like(cb.lower(root.get("clientName")), pattern);
                predicates.add(cb.or(nameLike, clientLike));
            }

            if (status != null && !status.isBlank()) {
                try {
                    ProjectStatus ps = ProjectStatus.valueOf(status);
                    predicates.add(cb.equal(root.get("status"), ps));
                } catch (IllegalArgumentException ignored) {}
            } else {
                // Par defaut on masque les projets archives (suppression douce)
                predicates.add(cb.notEqual(root.get("status"), ProjectStatus.ARCHIVED));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
        Page<Project> projectPage = projectRepository.findAll(spec, pageRequest);

        List<ProjectDto> items = projectPage.getContent().stream()
                .map(this::toProjectDto)
                .collect(Collectors.toList());

        return PageResponseDto.<ProjectDto>builder()
                .items(items)
                .total(projectPage.getTotalElements())
                .page(projectPage.getNumber())
                .size(projectPage.getSize())
                .totalPages(projectPage.getTotalPages())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  5.  GET /api/manager/projects/{id}
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ProjectDto getProjectDetail(Long managerId, Long projectId) {
        Project project = projectRepository.findByIdAndManagerId(projectId, managerId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable ou non autorise (id=" + projectId + ")"));
        return toProjectDto(project);
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════

    /** Collecte tous les EmployeeProfiles : assignés aux projets du manager + rattachés via EmployeeProfile.manager */
    private Set<EmployeeProfile> collectTeamProfiles(List<Project> projects, Long managerId) {
        Set<EmployeeProfile> profiles = new LinkedHashSet<>();

        // 1. Via les assignments sur les projets du manager
        for (Project p : projects) {
            for (Assignment a : p.getAssignments()) {
                if (a.getEmployeeProfile() != null) {
                    profiles.add(a.getEmployeeProfile());
                }
            }
        }

        // 2. Via EmployeeProfile.manager (collaborateurs approuvés mais pas encore assignés à un projet)
        profiles.addAll(employeeProfileRepository.findByManagerId(managerId));

        return profiles;
    }

    /** STAFFED / AVAILABLE / SOON_AVAILABLE */
    private String computeAvailability(EmployeeProfile ep, LocalDate now, LocalDate soonLimit) {
        boolean hasActive = ep.getAssignments().stream()
                .anyMatch(a -> a.getStatus() == AssignmentStatus.ACTIVE
                        && (a.getEndDate() == null || !a.getEndDate().isBefore(now)));

        if (!hasActive) return "AVAILABLE";

        boolean endingSoon = ep.getAssignments().stream()
                .anyMatch(a -> a.getStatus() == AssignmentStatus.ACTIVE
                        && a.getEndDate() != null
                        && !a.getEndDate().isBefore(now)
                        && !a.getEndDate().isAfter(soonLimit));

        return endingSoon ? "SOON_AVAILABLE" : "STAFFED";
    }

    private String determineAssignmentType(Assignment a, LocalDate now) {
        if (a.getStatus() == AssignmentStatus.COMPLETED) return "COMPLETED";
        if (a.getEndDate() != null && !a.getEndDate().isAfter(now.plusDays(SOON_AVAILABLE_DAYS))) return "ENDING_SOON";
        return "ASSIGNED";
    }

    private String getCollaboratorName(Assignment a) {
        if (a.getEmployeeProfile() == null || a.getEmployeeProfile().getUser() == null) return "?";
        User u = a.getEmployeeProfile().getUser();
        return u.getFirstName() + " " + u.getLastName();
    }

    // ─── Mappers ──────────────────────────────────────────────

    private CollaboratorDto toCollaboratorDto(EmployeeProfile ep, LocalDate now, LocalDate soonLimit) {
        User user = ep.getUser();
        String avail = computeAvailability(ep, now, soonLimit);

        // Current project
        String currentProject = ep.getAssignments().stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE
                        && (a.getEndDate() == null || !a.getEndDate().isBefore(now)))
                .map(Assignment::getProjectName)
                .findFirst().orElse(null);

        // Available from date
        String availableFrom = null;
        if ("SOON_AVAILABLE".equals(avail)) {
            availableFrom = ep.getAssignments().stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE && a.getEndDate() != null)
                    .map(Assignment::getEndDate)
                    .max(LocalDate::compareTo)
                    .map(d -> d.plusDays(1).format(D_FMT))
                    .orElse(null);
        }

        List<String> skills = ep.getSkills().stream()
                .map(Skill::getName)
                .collect(Collectors.toList());

        List<CollaboratorAssignmentDto> assignments = ep.getAssignments().stream()
                .map(this::toCollaboratorAssignmentDto)
                .collect(Collectors.toList());

        return CollaboratorDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(ep.getPhone())
                .department(ep.getDepartment())
                .skills(skills)
                .availability(avail)
                .currentProject(currentProject)
                .availableFrom(availableFrom)
                .assignments(assignments)
                .build();
    }

    private CollaboratorAssignmentDto toCollaboratorAssignmentDto(Assignment a) {
        return CollaboratorAssignmentDto.builder()
                .id(a.getId())
                .projectName(a.getProjectName())
                .clientName(a.getClientName())
                .roleName(a.getRoleName())
                .startDate(a.getStartDate() != null ? a.getStartDate().format(D_FMT) : null)
                .endDate(a.getEndDate() != null ? a.getEndDate().format(D_FMT) : null)
                .status(a.getStatus() != null ? a.getStatus().name() : null)
                .build();
    }

    private ProjectDto toProjectDto(Project p) {
        List<ProjectMemberDto> team = p.getAssignments().stream()
                .filter(a -> a.getEmployeeProfile() != null && a.getEmployeeProfile().getUser() != null)
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
                .map(a -> {
                    User u = a.getEmployeeProfile().getUser();
                    List<String> skills = a.getEmployeeProfile().getSkills().stream()
                            .map(Skill::getName).collect(Collectors.toList());
                    return ProjectMemberDto.builder()
                            .id(u.getId())
                            .assignmentId(a.getId())
                            .firstName(u.getFirstName())
                            .lastName(u.getLastName())
                            .role(a.getRoleName())
                            .skills(skills)
                            .build();
                })
                .collect(Collectors.toList());

        return ProjectDto.builder()
                .id(p.getId())
                .name(p.getName())
                .clientName(p.getClientName())
                .description(p.getDescription())
                .status(p.getStatus().name())
                .startDate(p.getStartDate() != null ? p.getStartDate().format(D_FMT) : null)
                .endDate(p.getEndDate() != null ? p.getEndDate().format(D_FMT) : null)
                .teamSize(team.size())
                .team(team)
                .technologies(p.getTechnologyList())
                .build();
    }

    // ─── Filters ──────────────────────────────────────────────

    private boolean matchSearch(CollaboratorDto dto, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.toLowerCase();
        return dto.getFirstName().toLowerCase().contains(q)
                || dto.getLastName().toLowerCase().contains(q)
                || dto.getEmail().toLowerCase().contains(q);
    }

    private boolean matchSkill(CollaboratorDto dto, String skill) {
        if (skill == null || skill.isBlank()) return true;
        return dto.getSkills() != null && dto.getSkills().stream()
                .anyMatch(s -> s.equalsIgnoreCase(skill));
    }

    private boolean matchAvailability(CollaboratorDto dto, String availability) {
        if (availability == null || availability.isBlank()) return true;
        return availability.equalsIgnoreCase(dto.getAvailability());
    }
    // ═══════════════════════════════════════════════════════════
    //  CALENDAR : FullCalendar Events & Resources
    // ═══════════════════════════════════════════════════════════

    /**
     * Retourne les événements calendrier (assignments + projets) pour FullCalendar.
     * Chaque assignment = une barre colorée sur la timeline du collaborateur.
     * Chaque projet = une barre de fond.
     */
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getCalendarEvents(Long managerId, LocalDate start, LocalDate end) {
        List<Project> projects = projectRepository.findByManagerId(managerId);
        LocalDate now = LocalDate.now();
        LocalDate soonLimit = now.plusDays(SOON_AVAILABLE_DAYS);
        List<CalendarEventDto> events = new ArrayList<>();

        for (Project p : projects) {
            // Filtrer les projets qui chevauchent la période demandée
            if (p.getStartDate() != null && p.getEndDate() != null) {
                if (p.getEndDate().isBefore(start) || p.getStartDate().isAfter(end)) continue;
            }

            // Événement projet (barre de fond)
            events.add(CalendarEventDto.builder()
                    .id("project-" + p.getId())
                    .title(p.getName() + " (" + p.getStatus().name() + ")")
                    .start(p.getStartDate() != null ? p.getStartDate().format(D_FMT) : null)
                    .end(p.getEndDate() != null ? p.getEndDate().plusDays(1).format(D_FMT) : null)
                    .color(getProjectColor(p.getStatus()))
                    .borderColor(getProjectColor(p.getStatus()))
                    .extendedProps(Map.of(
                            "type", "PROJECT",
                            "clientName", p.getClientName() != null ? p.getClientName() : "",
                            "status", p.getStatus().name(),
                            "teamSize", p.getAssignments().size()
                    ))
                    .build());

            // Événements assignments (1 par collaborateur affecté)
            for (Assignment a : p.getAssignments()) {
                if (a.getEmployeeProfile() == null || a.getEmployeeProfile().getUser() == null) continue;
                if (a.getStartDate() != null && a.getEndDate() != null) {
                    if (a.getEndDate().isBefore(start) || a.getStartDate().isAfter(end)) continue;
                }

                User user = a.getEmployeeProfile().getUser();
                String availability = computeAvailability(a.getEmployeeProfile(), now, soonLimit);
                String assignmentType = determineAssignmentType(a, now);

                events.add(CalendarEventDto.builder()
                        .id("assignment-" + a.getId())
                        .title(user.getFirstName() + " " + user.getLastName() + " → " + a.getRoleName())
                        .start(a.getStartDate() != null ? a.getStartDate().format(D_FMT) : null)
                        .end(a.getEndDate() != null ? a.getEndDate().plusDays(1).format(D_FMT) : null)
                        .resourceId("collab-" + user.getId())
                        .color(getAssignmentColor(assignmentType))
                        .borderColor(getAssignmentColor(assignmentType))
                        .extendedProps(Map.of(
                                "type", "ASSIGNMENT",
                                "projectName", a.getProjectName() != null ? a.getProjectName() : "",
                                "clientName", a.getClientName() != null ? a.getClientName() : "",
                                "collaboratorName", user.getFirstName() + " " + user.getLastName(),
                                "roleName", a.getRoleName() != null ? a.getRoleName() : "",
                                "status", a.getStatus().name(),
                                "availability", availability,
                                "assignmentType", assignmentType
                        ))
                        .build());
            }
        }

        return events;
    }

    /**
     * Retourne les ressources (collaborateurs) pour la vue Timeline de FullCalendar.
     * Chaque collaborateur = une ligne dans le calendrier.
     */
    @Transactional(readOnly = true)
    public List<CalendarResourceDto> getCalendarResources(Long managerId) {
        List<Project> projects = projectRepository.findByManagerId(managerId);
        Set<EmployeeProfile> teamProfiles = collectTeamProfiles(projects, managerId);
        LocalDate now = LocalDate.now();
        LocalDate soonLimit = now.plusDays(SOON_AVAILABLE_DAYS);

        return teamProfiles.stream()
                .map(ep -> {
                    User user = ep.getUser();
                    String availability = computeAvailability(ep, now, soonLimit);
                    return CalendarResourceDto.builder()
                            .id("collab-" + user.getId())
                            .title(user.getFirstName() + " " + user.getLastName())
                            .department(ep.getDepartment() != null ? ep.getDepartment() : "")
                            .availability(availability)
                            .build();
                })
                .sorted(Comparator.comparing(CalendarResourceDto::getTitle))
                .collect(Collectors.toList());
    }

    // ─── Couleurs Calendar ────────────────────────────────────

    private String getAssignmentColor(String type) {
        return switch (type) {
            case "ASSIGNED" -> "#4CAF50";       // Vert : en cours
            case "ENDING_SOON" -> "#FF9800";    // Orange : finit bientôt
            case "COMPLETED" -> "#9E9E9E";      // Gris : terminé
            default -> "#2196F3";               // Bleu par défaut
        };
    }

    private String getProjectColor(ProjectStatus status) {
        return switch (status) {
            case ACTIVE -> "#1976D2";            // Bleu foncé
            case PLANNED -> "#FF9800";           // Orange
            case ON_HOLD -> "#F44336";           // Rouge
            case COMPLETED -> "#9E9E9E";         // Gris
            case ARCHIVED -> "#616161";          // Gris fonce
        };
    }

    // ═══════════════════════════════════════════════════════════
    //  ACTIONS : Créer projet, Assigner collaborateur, Désassigner
    // ═══════════════════════════════════════════════════════════

    @Transactional
    public ProjectDto createProject(Long managerId, CreateProjectRequest req) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager introuvable"));

        ProjectStatus status = ProjectStatus.PLANNED;
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            try { status = ProjectStatus.valueOf(req.getStatus()); } catch (Exception ignored) {}
        }

        Project project = Project.builder()
                .name(req.getName())
                .description(req.getDescription())
                .clientName(req.getClientName())
                .startDate(LocalDate.parse(req.getStartDate(), D_FMT))
                .endDate(req.getEndDate() != null && !req.getEndDate().isBlank() ? LocalDate.parse(req.getEndDate(), D_FMT) : null)
                .status(status)
                .manager(manager)
                .technologies(req.getTechnologies())
                .neededRessource(req.getNeededRessource())
                .build();

        project = projectRepository.save(project);
        return mapProjectToDto(project);
    }

    @Transactional
    public ProjectDto updateProject(Long managerId, Long projectId, CreateProjectRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable"));
        if (!project.getManager().getId().equals(managerId)) {
            throw new SecurityException("Ce projet ne vous appartient pas");
        }

        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project.setClientName(req.getClientName());
        project.setStartDate(LocalDate.parse(req.getStartDate(), D_FMT));
        project.setEndDate(req.getEndDate() != null && !req.getEndDate().isBlank() ? LocalDate.parse(req.getEndDate(), D_FMT) : null);
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            try { project.setStatus(ProjectStatus.valueOf(req.getStatus())); } catch (Exception ignored) {}
        }
        project.setTechnologies(req.getTechnologies());
        project.setNeededRessource(req.getNeededRessource());

        return mapProjectToDto(projectRepository.save(project));
    }

    @Transactional
    public void assignCollaborator(Long managerId, Long projectId, AssignCollaboratorRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable"));
        if (!project.getManager().getId().equals(managerId)) {
            throw new SecurityException("Ce projet ne vous appartient pas");
        }

        User collaborator = userRepository.findById(req.getCollaboratorId())
                .orElseThrow(() -> new IllegalArgumentException("Collaborateur introuvable"));

        EmployeeProfile profile = employeeProfileRepository.findByUserId(collaborator.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profil collaborateur introuvable"));

        // Vérifier si déjà assigné au même projet en ACTIVE
        boolean alreadyAssigned = profile.getAssignments().stream()
                .anyMatch(a -> a.getProject() != null
                        && a.getProject().getId().equals(projectId)
                        && a.getStatus() == AssignmentStatus.ACTIVE);
        if (alreadyAssigned) {
            throw new IllegalStateException("Ce collaborateur est déjà assigné à ce projet");
        }

        Assignment assignment = Assignment.builder()
                .employeeProfile(profile)
                .project(project)
                .projectName(project.getName())
                .clientName(project.getClientName())
                .roleName(req.getRoleName())
                .startDate(LocalDate.parse(req.getStartDate(), D_FMT))
                .endDate(req.getEndDate() != null && !req.getEndDate().isBlank() ? LocalDate.parse(req.getEndDate(), D_FMT) : null)
                .status(AssignmentStatus.ACTIVE)
                .build();

        assignmentRepository.save(assignment);
    }

    @Transactional
    public void unassignCollaborator(Long managerId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment introuvable"));

        // Vérifier que le projet appartient au manager
        if (assignment.getProject() == null || !assignment.getProject().getManager().getId().equals(managerId)) {
            throw new SecurityException("Vous n'êtes pas autorisé à modifier cet assignment");
        }

        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setEndDate(LocalDate.now());
        assignmentRepository.save(assignment);
    }

    @Transactional
    public void updateProjectStatus(Long managerId, Long projectId, String newStatus) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable"));
        if (!project.getManager().getId().equals(managerId)) {
            throw new SecurityException("Ce projet ne vous appartient pas");
        }
        project.setStatus(ProjectStatus.valueOf(newStatus));
        projectRepository.save(project);
    }

    /**
     * Suppression douce : marque le projet comme ARCHIVED et clot tous les
     * assignments encore ACTIFS sur ce projet. Les donnees historiques
     * (rapports, calendrier passe) restent disponibles.
     */
    @Transactional
    public void archiveProject(Long managerId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable"));
        if (!project.getManager().getId().equals(managerId)) {
            throw new SecurityException("Ce projet ne vous appartient pas");
        }
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Ce projet est deja archive");
        }

        LocalDate today = LocalDate.now();
        for (Assignment a : project.getAssignments()) {
            if (a.getStatus() == AssignmentStatus.ACTIVE) {
                a.setStatus(AssignmentStatus.COMPLETED);
                if (a.getEndDate() == null || a.getEndDate().isAfter(today)) {
                    a.setEndDate(today);
                }
            }
        }

        project.setStatus(ProjectStatus.ARCHIVED);
        projectRepository.save(project);
    }

    private ProjectDto mapProjectToDto(Project p) {
        List<ProjectMemberDto> team = p.getAssignments().stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
                .map(a -> {
                    User u = a.getEmployeeProfile().getUser();
                    return ProjectMemberDto.builder()
                            .id(u.getId())
                            .assignmentId(a.getId())
                            .firstName(u.getFirstName())
                            .lastName(u.getLastName())
                            .role(a.getRoleName())
                            .skills(a.getEmployeeProfile().getSkills() != null ?
                                    a.getEmployeeProfile().getSkills().stream().map(Skill::getName).toList() : List.of())
                            .build();
                }).toList();

        return ProjectDto.builder()
                .id(p.getId())
                .name(p.getName())
                .clientName(p.getClientName())
                .description(p.getDescription())
                .status(p.getStatus().name())
                .startDate(p.getStartDate() != null ? p.getStartDate().format(D_FMT) : null)
                .endDate(p.getEndDate() != null ? p.getEndDate().format(D_FMT) : null)
                .teamSize(team.size())
                .team(team)
                .technologies(p.getTechnologyList())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  GESTION DES CONGÉS DE L'ÉQUIPE
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponseDto<LeaveRequestManagerDto> getTeamLeaves(Long managerId, String status, int page, int size) {
        List<Long> teamUserIds = getTeamUserIds(managerId);
        if (teamUserIds.isEmpty()) {
            return PageResponseDto.<LeaveRequestManagerDto>builder()
                    .items(List.of()).total(0).page(page).size(size).build();
        }

        Page<LeaveRequest> pageResult;
        PageRequest pageable = PageRequest.of(page, size);
        if (status != null && !status.isBlank()) {
            LeaveStatus ls = LeaveStatus.valueOf(status.toUpperCase());
            pageResult = leaveRequestRepository.findByUserIdInAndStatus(teamUserIds, ls, pageable);
        } else {
            pageResult = leaveRequestRepository.findByUserIdIn(teamUserIds, pageable);
        }

        List<LeaveRequestManagerDto> items = pageResult.getContent().stream()
                .map(this::toLeaveManagerDto).toList();

        return PageResponseDto.<LeaveRequestManagerDto>builder()
                .items(items).total((int) pageResult.getTotalElements())
                .page(page).size(size).build();
    }

    @Transactional
    public void approveLeave(Long managerId, Long leaveId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager introuvable"));
        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Demande de congé introuvable"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Seules les demandes en attente peuvent être approuvées");
        }

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setReviewedBy(manager);
        leave.setReviewedAt(java.time.LocalDateTime.now());
        leaveRequestRepository.save(leave);

        // Notifier le collaborateur
        String msg = String.format("Votre demande de congé (%s) du %s au %s a été approuvée par %s %s",
                leave.getType().name(), leave.getStartDate(), leave.getEndDate(),
                manager.getFirstName(), manager.getLastName());
        notificationService.createNotification(msg, "LEAVE_APPROVED", leave.getUser());
    }

    @Transactional
    public void rejectLeave(Long managerId, Long leaveId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager introuvable"));
        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Demande de congé introuvable"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Seules les demandes en attente peuvent être rejetées");
        }

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setReviewedBy(manager);
        leave.setReviewedAt(java.time.LocalDateTime.now());
        leaveRequestRepository.save(leave);

        // Notifier le collaborateur
        String msg = String.format("Votre demande de congé (%s) du %s au %s a été refusée par %s %s",
                leave.getType().name(), leave.getStartDate(), leave.getEndDate(),
                manager.getFirstName(), manager.getLastName());
        notificationService.createNotification(msg, "LEAVE_REJECTED", leave.getUser());
    }

    private List<Long> getTeamUserIds(Long managerId) {
        // 1. Collaborateurs assignés aux projets du manager
        List<Project> projects = projectRepository.findByManagerId(managerId);
        List<Long> fromProjects = projects.stream()
                .flatMap(p -> p.getAssignments().stream())
                .map(a -> a.getEmployeeProfile().getUser().getId())
                .toList();

        // 2. Collaborateurs assignés directement au manager (via EmployeeProfile.manager)
        List<Long> fromProfile = employeeProfileRepository.findByManagerId(managerId).stream()
                .map(ep -> ep.getUser().getId())
                .toList();

        // Fusionner et dédupliquer
        return java.util.stream.Stream.concat(fromProjects.stream(), fromProfile.stream())
                .distinct().toList();
    }

    private LeaveRequestManagerDto toLeaveManagerDto(LeaveRequest l) {
        int days = (int) (l.getEndDate().toEpochDay() - l.getStartDate().toEpochDay()) + 1;
        return LeaveRequestManagerDto.builder()
                .id(l.getId())
                .collaboratorName(l.getUser().getFirstName() + " " + l.getUser().getLastName())
                .collaboratorEmail(l.getUser().getEmail())
                .type(l.getType().name())
                .startDate(l.getStartDate().format(D_FMT))
                .endDate(l.getEndDate().format(D_FMT))
                .days(days)
                .reason(l.getReason())
                .status(l.getStatus().name())
                .createdAt(l.getCreatedAt() != null ? l.getCreatedAt().toString() : null)
                .build();
    }
}
