package com.negzaoui.stuffing.repository;

import com.negzaoui.stuffing.entity.Project;
import com.negzaoui.stuffing.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    List<Project> findByManagerId(Long managerId);

    Optional<Project> findByIdAndManagerId(Long id, Long managerId);

    long countByManagerIdAndStatus(Long managerId, ProjectStatus status);

    /** Détache le manager de ses projets (lors de la suppression d'un user manager) */
    @Modifying
    @Query("UPDATE Project p SET p.manager = null WHERE p.manager.id = :managerId")
    void clearManager(@Param("managerId") Long managerId);
}



