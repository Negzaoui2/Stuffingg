package com.negzaoui.stuffing.service;

import com.negzaoui.stuffing.dto.collaborator.*;
import com.negzaoui.stuffing.dto.manager.CalendarEventDto;
import com.negzaoui.stuffing.dto.manager.PageResponseDto;
import com.negzaoui.stuffing.entity.*;
import com.negzaoui.stuffing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollaboratorService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final AssignmentRepository assignmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final SkillRepository skillRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Quotas annuels par defaut
    private static final int PAID_LEAVE_TOTAL = 25;
    private static final int RTT_TOTAL = 10;

    // ═══════════════════════════════════════════════════════════
    //  1. DASHBOARD
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CollaboratorDashboardDto getDashboard(Long userId) {
        User user = getUser(userId);
        EmployeeProfile profile = getProfile(userId);
        LocalDate now = LocalDate.now();

        // Current assignment
        CurrentAssignmentDto currentAssignment = null;
        Optional<Assignment> activeAssignment = profile.getAssignments().stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
                .filter(a -> a.getEndDate() == null || !a.getEndDate().isBefore(now))
                .findFirst();

        if (activeAssignment.isPresent()) {
            Assignment a = activeAssignment.get();
            LocalDate end = a.getEndDate() != null ? a.getEndDate() : now.plusYears(1);
            long totalDays = ChronoUnit.DAYS.between(a.getStartDate(), end);
            long elapsed = ChronoUnit.DAYS.between(a.getStartDate(), now);
            long remaining = ChronoUnit.DAYS.between(now, end);
            int progress = totalDays > 0 ? (int) Math.min(100, (elapsed * 100) / totalDays) : 0;

            currentAssignment = CurrentAssignmentDto.builder()
                    .projectName(a.getProjectName())
                    .clientName(a.getClientName())
                    .roleName(a.getRoleName())
                    .startDate(a.getStartDate().format(D_FMT))
                    .endDate(a.getEndDate() != null ? a.getEndDate().format(D_FMT) : null)
                    .daysRemaining((int) Math.max(0, remaining))
                    .progressPercent(Math.max(0, progress))
                    .build();
        }

        // Leave balance
        LeaveBalanceDto leaveBalance = computeLeaveBalance(userId);

        // Skill count
        int skillCount = profile.getSkills() != null ? profile.getSkills().size() : 0;

        // Upcoming events
        List<UpcomingEventDto> events = new ArrayList<>();
        // - Assignment ends
        profile.getAssignments().stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE && a.getEndDate() != null && a.getEndDate().isAfter(now))
                .sorted(Comparator.comparing(Assignment::getEndDate))
                .limit(3)
                .forEach(a -> events.add(UpcomingEventDto.builder()
                        .type("ASSIGNMENT_END")
                        .label("Fin mission " + a.getProjectName())
                        .date(a.getEndDate().format(D_FMT))
                        .build()));
        // - Approved leaves
        leaveRequestRepository.findByUserIdAndStatusIn(userId, List.of(LeaveStatus.APPROVED)).stream()
                .filter(l -> l.getStartDate().isAfter(now))
                .sorted(Comparator.comparing(LeaveRequest::getStartDate))
                .limit(3)
                .forEach(l -> events.add(UpcomingEventDto.builder()
                        .type("LEAVE_APPROVED")
                        .label(leaveTypeLabel(l.getType()))
                        .date(l.getStartDate().format(D_FMT))
                        .build()));
        events.sort(Comparator.comparing(UpcomingEventDto::getDate));

        // Recent notifications
        List<RecentNotificationDto> notifications = new ArrayList<>();
        try {
            notificationService.getAllNotifications(user).stream()
                    .limit(5)
                    .forEach(n -> notifications.add(RecentNotificationDto.builder()
                            .message(n.getMessage())
                            .date(n.getCreatedAt() != null ? n.getCreatedAt().format(DT_FMT) : "")
                            .type(n.getType() != null ? n.getType() : "INFO")
                            .build()));
        } catch (Exception ignored) {
            // pas de notifications
        }

        return CollaboratorDashboardDto.builder()
                .fullName(user.getFirstName() + " " + user.getLastName())
                .department(profile.getDepartment())
                .currentAssignment(currentAssignment)
                .leaveBalance(leaveBalance)
                .skillCount(skillCount)
                .upcomingEvents(events)
                .recentNotifications(notifications)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  2. ASSIGNMENTS
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponseDto<CollaboratorAssignmentDetailDto> getAssignments(Long userId, String status, int page, int size) {
        EmployeeProfile profile = getProfile(userId);
        List<Assignment> all = profile.getAssignments();

        // Filter by status
        List<Assignment> filtered = all;
        if (status != null && !status.isBlank()) {
            try {
                AssignmentStatus s = AssignmentStatus.valueOf(status.toUpperCase());
                filtered = all.stream().filter(a -> a.getStatus() == s).collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        // Sort by startDate desc
        filtered.sort(Comparator.comparing(Assignment::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())));

        // Paginate
        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<CollaboratorAssignmentDetailDto> items = filtered.subList(from, to).stream()
                .map(this::toAssignmentDto)
                .collect(Collectors.toList());

        return PageResponseDto.<CollaboratorAssignmentDetailDto>builder()
                .items(items).total(total).page(page).size(size).build();
    }

    @Transactional(readOnly = true)
    public CollaboratorAssignmentDetailDto getAssignmentDetail(Long userId, Long assignmentId) {
        EmployeeProfile profile = getProfile(userId);
        Assignment a = profile.getAssignments().stream()
                .filter(as -> as.getId().equals(assignmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Affectation introuvable (id=" + assignmentId + ")"));
        return toAssignmentDto(a);
    }

    /**
     * Le collaborateur marque lui-même son affectation comme terminée.
     * Met le statut à COMPLETED et notifie son manager.
     */
    @Transactional
    public CollaboratorAssignmentDetailDto completeAssignment(Long userId, Long assignmentId) {
        User user = getUser(userId);
        EmployeeProfile profile = getProfile(userId);

        Assignment a = profile.getAssignments().stream()
                .filter(as -> as.getId().equals(assignmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Affectation introuvable (id=" + assignmentId + ")"));

        if (a.getStatus() == AssignmentStatus.COMPLETED) {
            throw new IllegalStateException("Cette affectation est déjà terminée");
        }

        a.setStatus(AssignmentStatus.COMPLETED);
        assignmentRepository.save(a);

        // Notifier le manager du collaborateur
        User manager = profile.getManager();
        if (manager != null) {
            String message = String.format(
                    "%s %s a marqué sa tâche '%s' comme terminée",
                    user.getFirstName(), user.getLastName(), a.getProjectName());
            notificationService.createNotification(message, "ASSIGNMENT_COMPLETED", manager);
        }

        return toAssignmentDto(a);
    }

    // ═══════════════════════════════════════════════════════════
    //  3. CALENDAR
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<CalendarEventDto> getCalendarEvents(Long userId, LocalDate start, LocalDate end) {
        EmployeeProfile profile = getProfile(userId);
        List<CalendarEventDto> events = new ArrayList<>();

        // Assignments
        for (Assignment a : profile.getAssignments()) {
            if (a.getStartDate() != null && a.getEndDate() != null) {
                if (a.getEndDate().isBefore(start) || a.getStartDate().isAfter(end)) continue;
            }
            String color = a.getStatus() == AssignmentStatus.ACTIVE ? "#3B82F6" :
                           a.getStatus() == AssignmentStatus.COMPLETED ? "#9E9E9E" : "#F59E0B";

            events.add(CalendarEventDto.builder()
                    .id("assignment-" + a.getId())
                    .title(a.getProjectName() + " - " + a.getRoleName())
                    .start(a.getStartDate() != null ? a.getStartDate().format(D_FMT) : null)
                    .end(a.getEndDate() != null ? a.getEndDate().plusDays(1).format(D_FMT) : null)
                    .color(color)
                    .extendedProps(Map.of(
                            "type", "ASSIGNMENT",
                            "projectName", nvl(a.getProjectName()),
                            "clientName", nvl(a.getClientName()),
                            "roleName", nvl(a.getRoleName()),
                            "status", a.getStatus().name()
                    ))
                    .build());
        }

        // Leaves
        List<LeaveRequest> leaves = leaveRequestRepository.findByUserIdAndPeriod(userId, start, end);
        for (LeaveRequest l : leaves) {
            String color = switch (l.getStatus()) {
                case APPROVED -> "#10B981";
                case PENDING -> "#F59E0B";
                case REJECTED -> "#EF4444";
            };
            String statusLabel = l.getStatus() == LeaveStatus.PENDING ? " (en attente)" :
                                 l.getStatus() == LeaveStatus.REJECTED ? " (refusé)" : "";

            events.add(CalendarEventDto.builder()
                    .id("leave-" + l.getId())
                    .title(leaveTypeLabel(l.getType()) + statusLabel)
                    .start(l.getStartDate().format(D_FMT))
                    .end(l.getEndDate().plusDays(1).format(D_FMT))
                    .color(color)
                    .extendedProps(Map.of(
                            "type", "LEAVE",
                            "leaveType", l.getType().name(),
                            "status", l.getStatus().name(),
                            "reason", nvl(l.getReason())
                    ))
                    .build());
        }

        return events;
    }

    // ═══════════════════════════════════════════════════════════
    //  4. LEAVES (CONGÉS)
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponseDto<LeaveRequestDto> getLeaves(Long userId, String status, int page, int size) {
        PageRequest pageReq = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LeaveRequest> pageResult;

        if (status != null && !status.isBlank()) {
            try {
                LeaveStatus ls = LeaveStatus.valueOf(status.toUpperCase());
                pageResult = leaveRequestRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, ls, pageReq);
            } catch (IllegalArgumentException e) {
                pageResult = leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageReq);
            }
        } else {
            pageResult = leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageReq);
        }

        List<LeaveRequestDto> items = pageResult.getContent().stream()
                .map(this::toLeaveDto)
                .collect(Collectors.toList());

        return PageResponseDto.<LeaveRequestDto>builder()
                .items(items).total((int) pageResult.getTotalElements())
                .page(pageResult.getNumber()).size(pageResult.getSize()).build();
    }

    @Transactional
    public LeaveRequestDto createLeave(Long userId, CreateLeaveRequest req) {
        User user = getUser(userId);

        // Parse type
        LeaveType type;
        try {
            type = LeaveType.valueOf(req.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type de conge invalide: " + req.getType());
        }

        // Validations
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new IllegalArgumentException("La date de fin doit etre apres la date de debut");
        }

        if (leaveRequestRepository.existsOverlapping(userId, req.getStartDate(), req.getEndDate())) {
            throw new IllegalStateException("Un conge existe deja sur cette periode");
        }

        // Check balance
        int daysRequested = (int) (req.getEndDate().toEpochDay() - req.getStartDate().toEpochDay()) + 1;
        if (type == LeaveType.PAID_LEAVE) {
            int used = leaveRequestRepository.sumDaysByTypeAndUser(userId, LeaveType.PAID_LEAVE.name());
            if (used + daysRequested > PAID_LEAVE_TOTAL) {
                throw new IllegalStateException("Solde de conges payes insuffisant (reste: " + (PAID_LEAVE_TOTAL - used) + " jours)");
            }
        } else if (type == LeaveType.RTT) {
            int used = leaveRequestRepository.sumDaysByTypeAndUser(userId, LeaveType.RTT.name());
            if (used + daysRequested > RTT_TOTAL) {
                throw new IllegalStateException("Solde de RTT insuffisant (reste: " + (RTT_TOTAL - used) + " jours)");
            }
        }

        LeaveRequest leave = LeaveRequest.builder()
                .user(user)
                .type(type)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .reason(req.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        leave = leaveRequestRepository.save(leave);

        // Notifier tous les Delivery Managers
        String notifMessage = String.format("Nouvelle demande de congé (%s) de %s %s du %s au %s",
                type.name(), user.getFirstName(), user.getLastName(),
                req.getStartDate(), req.getEndDate());
        List<User> managers = userRepository.findByRole(Role.DELIVERY_MANAGER);
        for (User manager : managers) {
            notificationService.createNotification(notifMessage, "LEAVE_REQUEST", manager);
        }

        return toLeaveDto(leave);
    }

    @Transactional
    public void cancelLeave(Long userId, Long leaveId) {
        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Demande de conge introuvable (id=" + leaveId + ")"));

        if (!leave.getUser().getId().equals(userId)) {
            throw new SecurityException("Vous ne pouvez supprimer que vos propres demandes");
        }

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Seules les demandes en attente peuvent etre annulees (statut actuel: " + leave.getStatus() + ")");
        }

        leaveRequestRepository.delete(leave);
    }

    @Transactional(readOnly = true)
    public LeaveBalanceDto getLeaveBalance(Long userId) {
        return computeLeaveBalance(userId);
    }

    // ═══════════════════════════════════════════════════════════
    //  5. PROFILE & SKILLS
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CollaboratorProfileDto getProfile(Long userId, boolean dummy) {
        User user = getUser(userId);
        EmployeeProfile profile = getProfile(userId);

        List<SkillDto> skills = profile.getSkills() != null ?
                profile.getSkills().stream()
                        .map(s -> SkillDto.builder()
                                .id(s.getId())
                                .name(s.getName())
                                .level(s.getLevel() != null ? s.getLevel().name() : "INTERMEDIATE")
                                .build())
                        .collect(Collectors.toList()) :
                List.of();

        return CollaboratorProfileDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(profile.getPhone())
                .department(profile.getDepartment())
                .role(user.getRole().name())
                .skills(skills)
                .joinedAt(user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().format(D_FMT) : null)
                .build();
    }

    @Transactional
    public CollaboratorProfileDto updateProfile(Long userId, UpdateProfileRequest req) {
        EmployeeProfile profile = getProfile(userId);
        if (req.getPhone() != null) {
            profile.setPhone(req.getPhone());
        }
        employeeProfileRepository.save(profile);
        return getProfile(userId, true);
    }

    @Transactional
    public CollaboratorProfileDto updateSkills(Long userId, UpdateSkillsRequest req) {
        EmployeeProfile profile = getProfile(userId);

        // Clear existing skills
        profile.getSkills().clear();
        employeeProfileRepository.saveAndFlush(profile);

        // Déduplication des noms (après normalisation) pour éviter "java" + "Java" en double
        java.util.Set<String> alreadyAdded = new java.util.HashSet<>();

        // Add new skills
        for (SkillDto dto : req.getSkills()) {
            String normalizedName = com.negzaoui.stuffing.util.SkillNameNormalizer.normalize(dto.getName());
            if (normalizedName == null) continue;             // nom vide ou null → ignoré
            if (!alreadyAdded.add(normalizedName)) continue;  // doublon dans la requête → ignoré

            SkillLevel level;
            try {
                level = dto.getLevel() != null ? SkillLevel.valueOf(dto.getLevel().toUpperCase()) : SkillLevel.INTERMEDIATE;
            } catch (IllegalArgumentException e) {
                level = SkillLevel.INTERMEDIATE;
            }

            Skill skill = Skill.builder()
                    .name(normalizedName)
                    .level(level)
                    .employeeProfile(profile)
                    .build();
            profile.getSkills().add(skill);
        }

        employeeProfileRepository.save(profile);
        return getProfile(userId, true);
    }

    @Transactional(readOnly = true)
    public List<String> suggestSkills(String query) {
        if (query == null || query.isBlank()) return List.of();
        // On normalise aussi la query : si l'utilisateur tape "reactjs", on cherche les "React" existants.
        String normalized = com.negzaoui.stuffing.util.SkillNameNormalizer.normalize(query);
        return skillRepository.findDistinctNamesByQuery(normalized != null ? normalized : query);
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    private EmployeeProfile getProfile(Long userId) {
        return employeeProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profil employe introuvable"));
    }

    private LeaveBalanceDto computeLeaveBalance(Long userId) {
        int paidUsed = leaveRequestRepository.sumDaysByTypeAndUser(userId, LeaveType.PAID_LEAVE.name());
        int rttUsed = leaveRequestRepository.sumDaysByTypeAndUser(userId, LeaveType.RTT.name());
        int sickUsed = leaveRequestRepository.sumDaysByTypeAndUser(userId, LeaveType.SICK_LEAVE.name());

        return LeaveBalanceDto.builder()
                .paidLeaveTotal(PAID_LEAVE_TOTAL)
                .paidLeaveUsed(paidUsed)
                .paidLeaveRemaining(PAID_LEAVE_TOTAL - paidUsed)
                .rttTotal(RTT_TOTAL)
                .rttUsed(rttUsed)
                .rttRemaining(RTT_TOTAL - rttUsed)
                .sickLeaveTaken(sickUsed)
                .build();
    }

    private CollaboratorAssignmentDetailDto toAssignmentDto(Assignment a) {
        List<String> techs = List.of();
        if (a.getProject() != null) {
            techs = a.getProject().getTechnologyList();
        }
        return CollaboratorAssignmentDetailDto.builder()
                .id(a.getId())
                .projectName(a.getProjectName())
                .clientName(a.getClientName())
                .roleName(a.getRoleName())
                .startDate(a.getStartDate() != null ? a.getStartDate().format(D_FMT) : null)
                .endDate(a.getEndDate() != null ? a.getEndDate().format(D_FMT) : null)
                .status(a.getStatus().name())
                .technologies(techs)
                .build();
    }

    private LeaveRequestDto toLeaveDto(LeaveRequest l) {
        String reviewedBy = null;
        if (l.getReviewedBy() != null) {
            reviewedBy = l.getReviewedBy().getFirstName() + " " + l.getReviewedBy().getLastName();
        }
        return LeaveRequestDto.builder()
                .id(l.getId())
                .type(l.getType().name())
                .startDate(l.getStartDate().format(D_FMT))
                .endDate(l.getEndDate().format(D_FMT))
                .days(l.getDays())
                .reason(l.getReason())
                .status(l.getStatus().name())
                .reviewedBy(reviewedBy)
                .createdAt(l.getCreatedAt() != null ? l.getCreatedAt().format(DT_FMT) : null)
                .build();
    }

    private String leaveTypeLabel(LeaveType type) {
        return switch (type) {
            case PAID_LEAVE -> "Conge paye";
            case RTT -> "RTT";
            case SICK_LEAVE -> "Arret maladie";
            case UNPAID_LEAVE -> "Conge sans solde";
        };
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}

