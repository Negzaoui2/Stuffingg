package com.negzaoui.stuffing.dto.admin;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveStatsResponse {
    private long totalRequests;
    private long pending;
    private long approved;
    private long rejected;
    private List<LeaveTypeStatDto> byType;
    private double absenteeismRate;
    private List<MonthlyDistributionDto> monthlyDistribution;
}

