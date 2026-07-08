package com.negzaoui.stuffing.dto.admin;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveDetailAdminDto {
    private Long id;
    private Long collaboratorId;
    private String collaboratorName;
    private String collaboratorEmail;
    private String department;
    private String type;
    private String startDate;
    private String endDate;
    private int days;
    private String reason;
    private String status;
    private String createdAt;
    private String reviewedBy;
    private String reviewedAt;
}

