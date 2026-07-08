package com.negzaoui.stuffing.dto.collaborator;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDto {
    private Long id;
    private String type;
    private String startDate;
    private String endDate;
    private int days;
    private String reason;
    private String status;
    private String reviewedBy;
    private String createdAt;
}

