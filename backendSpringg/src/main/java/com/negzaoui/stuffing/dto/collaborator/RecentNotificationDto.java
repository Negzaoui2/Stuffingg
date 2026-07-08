package com.negzaoui.stuffing.dto.collaborator;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentNotificationDto {
    private String message;
    private String date;
    private String type;
}

