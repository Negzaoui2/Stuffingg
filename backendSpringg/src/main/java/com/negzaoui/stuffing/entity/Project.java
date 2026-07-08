package com.negzaoui.stuffing.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 2000)
    private String description;

    private String clientName;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PLANNED;

    @Column(length = 1000)
    private String neededRessource;

    /** Le Delivery Manager responsable de ce projet */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    /** Technologies utilisees (stockees en CSV ou liste) */
    @Column(length = 1000)
    private String technologies;

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assignment> assignments = new ArrayList<>();

    /** Helper : split technologies CSV */
    @Transient
    public List<String> getTechnologyList() {
        if (technologies == null || technologies.isBlank()) return List.of();
        return List.of(technologies.split("\\s*,\\s*"));
    }
}
