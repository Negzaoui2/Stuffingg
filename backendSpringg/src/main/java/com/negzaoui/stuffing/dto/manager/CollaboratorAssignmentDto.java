package com.negzaoui.stuffing.dto.manager;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorAssignmentDto {
    private Long id;
    private String projectName;
    private String clientName;
    private String roleName;
    private String startDate;
    private String endDate;
    private String status;
}

