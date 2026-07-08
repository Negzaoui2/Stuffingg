package com.negzaoui.stuffing.dto.admin;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDto {

    private Long id;
    private String projectName;
    private String clientName;
    private String roleName;
    private String startDate;
    private String endDate;
    private String status;
}

