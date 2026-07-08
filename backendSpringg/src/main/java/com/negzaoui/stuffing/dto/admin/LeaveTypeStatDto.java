package com.negzaoui.stuffing.dto.admin;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeStatDto {
    private String type;
    private String label;
    private long count;
    private long totalDays;
}

