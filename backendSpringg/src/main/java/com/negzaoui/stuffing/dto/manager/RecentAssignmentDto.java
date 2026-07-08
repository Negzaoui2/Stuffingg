package com.negzaoui.stuffing.dto.manager;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentAssignmentDto {
    private String collaboratorName;
    private String projectName;
    private String date;
    private String type;
}

