package com.negzaoui.stuffing.dto.admin;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyDistributionDto {
    private String month;
    private long totalDays;
    private long requestCount;
}

