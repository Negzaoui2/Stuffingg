package com.negzaoui.stuffing.dto.admin;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AbsenteeismResponse {
    private double globalRate;
    private String period;
    private List<DepartmentAbsenteeismDto> byDepartment;
}

