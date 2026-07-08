package com.negzaoui.stuffing.service;

import com.negzaoui.stuffing.entity.Assignment;
import com.negzaoui.stuffing.entity.AssignmentStatus;
import com.negzaoui.stuffing.entity.EmployeeProfile;
import com.negzaoui.stuffing.entity.User;
import com.negzaoui.stuffing.repository.AssignmentRepository;
import com.negzaoui.stuffing.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Tâche planifiée qui génère automatiquement des notifications temporelles
 * pour les managers :
 *   - affectation en retard (endDate dépassée mais statut encore ACTIVE)
 *   - collaborateur bientôt disponible (endDate dans les {@link #SOON_AVAILABLE_DAYS} jours)
 *
 * Le statut des affectations n'est jamais modifié automatiquement :
 * seule la clôture manuelle par le collaborateur change le statut.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentScheduler {

    private static final int SOON_AVAILABLE_DAYS = 7;
    private static final int ENDING_REMINDER_DAYS = 3;
    private static final String TYPE_OVERDUE = "ASSIGNMENT_OVERDUE";
    private static final String TYPE_SOON_AVAILABLE = "COLLABORATOR_SOON_AVAILABLE";
    private static final String TYPE_ENDING_SOON = "ASSIGNMENT_ENDING_SOON";

    private final AssignmentRepository assignmentRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /** Tous les jours à 06h00. */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void runDailyChecks() {
        LocalDate today = LocalDate.now();
        log.info("AssignmentScheduler: démarrage des vérifications quotidiennes ({})", today);

        notifyOverdueAssignments(today);
        notifySoonAvailableCollaborators(today);
        notifyCollaboratorsEndingSoon(today);

        log.info("AssignmentScheduler: vérifications quotidiennes terminées");
    }

    private void notifyOverdueAssignments(LocalDate today) {
        List<Assignment> overdue =
                assignmentRepository.findByStatusAndEndDateBefore(AssignmentStatus.ACTIVE, today);

        for (Assignment a : overdue) {
            if (notificationRepository.existsByAssignmentIdAndType(a.getId(), TYPE_OVERDUE)) {
                continue;
            }
            User manager = managerOf(a);
            if (manager == null) continue;

            String message = String.format(
                    "La tâche '%s' de %s a dépassé sa date de fin (%s)",
                    a.getProjectName(), collaboratorName(a), a.getEndDate());
            notificationService.createNotification(message, TYPE_OVERDUE, manager, a.getId());
        }
    }

    private void notifySoonAvailableCollaborators(LocalDate today) {
        LocalDate limit = today.plusDays(SOON_AVAILABLE_DAYS);
        List<Assignment> ending =
                assignmentRepository.findByStatusAndEndDateBetween(AssignmentStatus.ACTIVE, today, limit);

        for (Assignment a : ending) {
            if (notificationRepository.existsByAssignmentIdAndType(a.getId(), TYPE_SOON_AVAILABLE)) {
                continue;
            }
            User manager = managerOf(a);
            if (manager == null) continue;

            String message = String.format(
                    "%s sera bientôt disponible (fin de mission '%s' le %s)",
                    collaboratorName(a), a.getProjectName(), a.getEndDate());
            notificationService.createNotification(message, TYPE_SOON_AVAILABLE, manager, a.getId());
        }
    }

    private void notifyCollaboratorsEndingSoon(LocalDate today) {
        LocalDate target = today.plusDays(ENDING_REMINDER_DAYS);
        List<Assignment> ending =
                assignmentRepository.findByStatusAndEndDateBetween(AssignmentStatus.ACTIVE, target, target);

        for (Assignment a : ending) {
            if (notificationRepository.existsByAssignmentIdAndType(a.getId(), TYPE_ENDING_SOON)) {
                continue;
            }
            EmployeeProfile profile = a.getEmployeeProfile();
            User collaborator = profile != null ? profile.getUser() : null;
            if (collaborator == null) continue;

            String message = String.format(
                    "Votre tâche '%s' se termine dans %d jours (le %s)",
                    a.getProjectName(), ENDING_REMINDER_DAYS, a.getEndDate());
            notificationService.createNotification(message, TYPE_ENDING_SOON, collaborator, a.getId());
        }
    }

    private User managerOf(Assignment a) {
        EmployeeProfile profile = a.getEmployeeProfile();
        return profile != null ? profile.getManager() : null;
    }

    private String collaboratorName(Assignment a) {
        EmployeeProfile profile = a.getEmployeeProfile();
        if (profile == null || profile.getUser() == null) return "Le collaborateur";
        User u = profile.getUser();
        return (u.getFirstName() + " " + u.getLastName()).trim();
    }
}
