package com.negzaoui.stuffing.service;

import com.negzaoui.stuffing.entity.Notification;
import com.negzaoui.stuffing.entity.Role;
import com.negzaoui.stuffing.entity.User;
import com.negzaoui.stuffing.repository.NotificationRepository;
import com.negzaoui.stuffing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ─── Création ────────────────────────────────────────────

    /**
     * Crée une notification ciblée pour un utilisateur précis.
     */
    public Notification createNotification(String message, String type, User targetUser) {
        Notification notification = Notification.builder()
                .message(message)
                .type(type)
                .targetUser(targetUser)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Crée une notification ciblée liée à une affectation précise.
     */
    public Notification createNotification(String message, String type, User targetUser, Long assignmentId) {
        Notification notification = Notification.builder()
                .message(message)
                .type(type)
                .targetUser(targetUser)
                .assignmentId(assignmentId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Crée une notification pour TOUS les ADMIN (ex: nouvelle demande de compte).
     */
    @Transactional
    public void notifyAdmins(String message, String type) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        if (admins.isEmpty()) {
            log.warn("Aucun ADMIN trouvé pour recevoir la notification: {}", message);
        }
        for (User admin : admins) {

            Notification notification = Notification.builder()
                    .message(message)
                    .type(type)
                    .targetUser(admin)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            System.out.println("Saved notification for admin id = " + admin.getId());
        }
        notificationRepository.flush();
        log.info("Notification envoyée à {} admin(s): {}", admins.size(), message);
    }

    /**
     * Crée une notification broadcast (sans cible précise).
     */
    public Notification createBroadcastNotification(String message, String type) {
        Notification notification = Notification.builder()
                .message(message)
                .type(type)
                .targetUser(null)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    // ─── Lecture ─────────────────────────────────────────────

    /** Notifications non lues d'un utilisateur */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByTargetUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    /** Toutes les notifications d'un utilisateur */
    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(User user) {
        return notificationRepository.findByTargetUserOrderByCreatedAtDesc(user);
    }

    /** Nombre de non lues d'un utilisateur */
    @Transactional(readOnly = true)
    public long countUnread(User user) {
        return notificationRepository.countByTargetUserAndIsReadFalse(user);
    }

    // ─── Actions ─────────────────────────────────────────────

    /** Marquer une notification comme lue */
    @Transactional
    public Notification markAsRead(Long notificationId, User currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable"));

        // Vérifier que la notification appartient bien à l'utilisateur
        if (notification.getTargetUser() != null
                && !notification.getTargetUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Cette notification ne vous appartient pas");
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    /** Marquer toutes les notifications non lues d'un utilisateur comme lues */
    @Transactional
    public void markAllAsRead(User currentUser) {
        List<Notification> unread = notificationRepository
                .findByTargetUserAndIsReadFalseOrderByCreatedAtDesc(currentUser);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
