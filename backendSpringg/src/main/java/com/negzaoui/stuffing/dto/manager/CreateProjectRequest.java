package com.negzaoui.stuffing.dto.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String clientName;
    @NotNull
    private String startDate;   // ISO date "2026-05-01"
    private String endDate;     // nullable
    private String status;      // PLANNED, ACTIVE (default PLANNED)
    private String technologies; // CSV: "Java,Angular,Docker"
    private String neededRessource;
}

