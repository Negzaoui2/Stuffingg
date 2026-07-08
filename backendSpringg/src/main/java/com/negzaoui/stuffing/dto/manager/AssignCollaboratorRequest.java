package com.negzaoui.stuffing.dto.manager;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCollaboratorRequest {
    @NotNull
    private Long collaboratorId;  // ID du User (pas du profile)
    private String roleName;      // Rôle dans le projet (ex: "Développeur Backend")
    @NotNull
    private String startDate;     // ISO date
    private String endDate;       // nullable
}

