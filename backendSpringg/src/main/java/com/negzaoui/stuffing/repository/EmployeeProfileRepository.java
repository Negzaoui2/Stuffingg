package com.negzaoui.stuffing.repository;

import com.negzaoui.stuffing.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    Optional<EmployeeProfile> findByUserId(Long userId);
    List<EmployeeProfile> findByManagerId(Long managerId);

    /** Profils rattachés à un département (filtrage par FK departement_id) */
    List<EmployeeProfile> findByDepartementId(Long departementId);

    /** Détache le manager des profils qu'il encadre (lors de la suppression d'un user manager) */
    @Modifying
    @Query("UPDATE EmployeeProfile ep SET ep.manager = null WHERE ep.manager.id = :managerId")
    void clearManager(@Param("managerId") Long managerId);
}



