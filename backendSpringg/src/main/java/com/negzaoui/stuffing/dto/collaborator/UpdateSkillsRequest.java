package com.negzaoui.stuffing.dto.collaborator;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSkillsRequest {
    @NotEmpty(message = "La liste de competences ne peut pas etre vide")
    private List<SkillDto> skills;
}

