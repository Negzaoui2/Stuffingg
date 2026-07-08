package com.negzaoui.stuffing.dto.collaborator;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CollaboratorProfileDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String department;
    private String role;
    private List<SkillDto> skills;
    private String joinedAt;
}

