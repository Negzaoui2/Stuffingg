package com.negzaoui.stuffing.repository;

import com.negzaoui.stuffing.entity.Notification;
import com.negzaoui.stuffing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Notifications non lues d'un utilisateur précis */
    List<Notification> findByTargetUserAndIsReadFalseOrderByCreatedAtDesc(User targetUser);

    /** Toutes les notifications d'un utilisateur */
    List<Notification> findByTargetUserOrderByCreatedAtDesc(User targetUser);

    /** Nombre de non lues pour un utilisateur */
    long countByTargetUserAndIsReadFalse(User targetUser);

    /** Notifications globales non lues (sans cible, broadcast) */
    List<Notification> findByTargetUserIsNullAndIsReadFalseOrderByCreatedAtDesc();

    /** Vérifie si une notification d'un type donné existe déjà pour une affectation (anti-doublon scheduler) */
    boolean existsByAssignmentIdAndType(Long assignmentId, String type);

    /** Ancienne méthode (gardée pour compatibilité) */
    List<Notification> findByIsReadFalse();

    long countByIsReadFalse();

    /** Supprime toutes les notifications ciblant un utilisateur (utilisé lors de la suppression d'un user) */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.targetUser.id = :userId")
    void deleteByTargetUserId(@Param("userId") Long userId);
}


