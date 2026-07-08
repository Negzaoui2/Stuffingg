package com.negzaoui.stuffing.dto.manager;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarResourceDto {
    private String id;
    private String title;
    private String department;
    private String availability;
}

