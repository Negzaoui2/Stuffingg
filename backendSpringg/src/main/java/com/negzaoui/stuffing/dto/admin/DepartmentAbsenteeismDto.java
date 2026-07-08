package com.negzaoui.stuffing.dto.admin;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentAbsenteeismDto {
    private String department;
    private double rate;
    private long totalDaysAbsent;
    private long collaboratorCount;
}
