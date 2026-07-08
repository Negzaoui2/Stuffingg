package com.negzaoui.stuffing.dto.manager;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardDto {
    private int totalCollaborators;
    private int activeProjects;
    private double occupancyRate;
    private int availableCollaborators;
    private int soonAvailableCollaborators;
    private List<SkillCountDto> skillDistribution;
    private List<ProjectStatusCountDto> projectStatusDistribution;
    private List<RecentAssignmentDto> recentAssignments;
}

