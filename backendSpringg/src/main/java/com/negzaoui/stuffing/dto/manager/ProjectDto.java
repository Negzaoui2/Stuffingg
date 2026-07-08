package com.negzaoui.stuffing.dto.manager;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private Long id;
    private String name;
    private String clientName;
    private String description;
    private String status;
    private String startDate;
    private String endDate;
    private int teamSize;
    private List<ProjectMemberDto> team;
    private List<String> technologies;
}

