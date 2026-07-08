package com.negzaoui.stuffing.dto.collaborator;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingEventDto {
    private String type;
    private String label;
    private String date;
}
