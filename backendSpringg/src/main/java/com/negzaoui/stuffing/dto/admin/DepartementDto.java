package com.negzaoui.stuffing.dto.admin;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartementDto {
    private Long id;
    private String name;
    private int employeeCount;
}

