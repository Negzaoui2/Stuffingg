package com.negzaoui.stuffing.repository;

import com.negzaoui.stuffing.entity.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long> {
    Optional<Departement> findByName(String name);
    boolean existsByName(String name);
}

