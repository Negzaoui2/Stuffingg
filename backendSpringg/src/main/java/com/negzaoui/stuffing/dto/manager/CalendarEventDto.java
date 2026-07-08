package com.negzaoui.stuffing.dto.manager;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventDto {
    private String id;
    private String title;
    private String start;
    private String end;
    private String resourceId;     // ID du collaborateur (pour la vue Timeline)
    private String color;
    private String borderColor;
    private Map<String, Object> extendedProps;
}

