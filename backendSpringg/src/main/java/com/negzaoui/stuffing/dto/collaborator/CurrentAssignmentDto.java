package com.negzaoui.stuffing.dto.collaborator;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentAssignmentDto {
    private String projectName;
    private String clientName;
    private String roleName;
    private String startDate;
    private String endDate;
    private int daysRemaining;
    private int progressPercent;
}

