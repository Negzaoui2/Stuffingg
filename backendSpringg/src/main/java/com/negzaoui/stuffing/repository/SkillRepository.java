package com.negzaoui.stuffing.repository;

import com.negzaoui.stuffing.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    @Query("SELECT DISTINCT s.name FROM Skill s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY s.name")
    List<String> findDistinctNamesByQuery(@Param("query") String query);
}
