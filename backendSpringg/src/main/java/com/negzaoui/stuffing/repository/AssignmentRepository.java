package com.negzaoui.stuffing.repository;

import com.negzaoui.stuffing.entity.Assignment;
import com.negzaoui.stuffing.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByProjectId(Long projectId);

    List<Assignment> findByProjectManagerId(Long managerId);

    @Query("SELECT a FROM Assignment a WHERE a.project.manager.id = :managerId ORDER BY a.startDate DESC")
    List<Assignment> findRecentByManagerId(@Param("managerId") Long managerId);

    /** Affectations encore actives dont la date de fin est déjà passée (en retard). */
    List<Assignment> findByStatusAndEndDateBefore(AssignmentStatus status, LocalDate date);

    /** Affectations encore actives dont la date de fin tombe dans un intervalle (bientôt finies). */
    List<Assignment> findByStatusAndEndDateBetween(AssignmentStatus status, LocalDate from, LocalDate to);
}

