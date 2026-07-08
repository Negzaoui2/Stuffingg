package com.negzaoui.stuffing.dto.manager;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatusCountDto {
    private String status;
    private int count;
}

