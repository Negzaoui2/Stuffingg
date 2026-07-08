package com.negzaoui.stuffing.dto.manager;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestManagerDto {
    private Long id;
    private String collaboratorName;
    private String collaboratorEmail;
    private String type;
    private String startDate;
    private String endDate;
    private int days;
    private String reason;
    private String status;
    private String createdAt;
}

