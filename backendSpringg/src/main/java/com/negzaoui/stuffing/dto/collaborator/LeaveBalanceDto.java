package com.negzaoui.stuffing.dto.collaborator;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceDto {
    private int paidLeaveRemaining;
    private int paidLeaveTotal;
    private int paidLeaveUsed;
    private int rttRemaining;
    private int rttTotal;
    private int rttUsed;
    private int sickLeaveTaken;
}

