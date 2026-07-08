package com.negzaoui.stuffing.repository;

import com.negzaoui.stuffing.entity.LeaveRequest;
import com.negzaoui.stuffing.entity.LeaveStatus;
import com.negzaoui.stuffing.entity.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<LeaveRequest> {

    Page<LeaveRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<LeaveRequest> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, LeaveStatus status, Pageable pageable);

    List<LeaveRequest> findByUserIdAndStatusIn(Long userId, List<LeaveStatus> statuses);

    List<LeaveRequest> findByUserId(Long userId);

    /**
     * Vérifie s'il y a un chevauchement avec un congé existant (non rejeté) du même user.
     */
    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l " +
           "WHERE l.user.id = :userId " +
           "AND l.status <> com.negzaoui.stuffing.entity.LeaveStatus.REJECTED " +
           "AND l.startDate <= :endDate " +
           "AND l.endDate >= :startDate")
    boolean existsOverlapping(@Param("userId") Long userId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    /**
     * Compte les jours de congé utilisés par type (APPROVED + PENDING).
     */
    @Query(value = "SELECT COALESCE(SUM(CAST((l.end_date - l.start_date) AS int) + 1), 0) FROM leave_requests l " +
           "WHERE l.user_id = :userId " +
           "AND l.type = :type " +
           "AND l.status IN ('APPROVED', 'PENDING')", nativeQuery = true)
    int sumDaysByTypeAndUser(@Param("userId") Long userId, @Param("type") String type);

    /**
     * Congés d'un user dans une période (pour le calendrier).
     */
    @Query("SELECT l FROM LeaveRequest l " +
           "WHERE l.user.id = :userId " +
           "AND l.status <> com.negzaoui.stuffing.entity.LeaveStatus.REJECTED " +
           "AND l.startDate <= :end " +
           "AND l.endDate >= :start " +
           "ORDER BY l.startDate")
    List<LeaveRequest> findByUserIdAndPeriod(@Param("userId") Long userId,
                                             @Param("start") LocalDate start,
                                             @Param("end") LocalDate end);

    /**
     * Congés d'une liste d'utilisateurs filtrés par statut.
     */
    @Query("SELECT l FROM LeaveRequest l WHERE l.user.id IN :userIds AND l.status = :status ORDER BY l.createdAt DESC")
    Page<LeaveRequest> findByUserIdInAndStatus(@Param("userIds") List<Long> userIds, @Param("status") LeaveStatus status, Pageable pageable);

    @Query("SELECT l FROM LeaveRequest l WHERE l.user.id IN :userIds ORDER BY l.createdAt DESC")
    Page<LeaveRequest> findByUserIdIn(@Param("userIds") List<Long> userIds, Pageable pageable);

    /** Supprime tous les congés d'un utilisateur (lors de la suppression d'un user) */
    @Modifying
    @Query("DELETE FROM LeaveRequest l WHERE l.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /** Détache le réviseur des congés qu'il a traités (lors de la suppression d'un user manager) */
    @Modifying
    @Query("UPDATE LeaveRequest l SET l.reviewedBy = null WHERE l.reviewedBy.id = :userId")
    void clearReviewedBy(@Param("userId") Long userId);
}
