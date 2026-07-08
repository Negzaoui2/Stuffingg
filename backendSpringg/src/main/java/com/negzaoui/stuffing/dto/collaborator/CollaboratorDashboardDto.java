package com.negzaoui.stuffing.dto.collaborator;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorDashboardDto {
    private String fullName;
    private String department;
    private CurrentAssignmentDto currentAssignment;
    private LeaveBalanceDto leaveBalance;
    private int skillCount;
    private List<UpcomingEventDto> upcomingEvents;
    private List<RecentNotificationDto> recentNotifications;
}

