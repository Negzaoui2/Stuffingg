package com.negzaoui.stuffing.dto.manager;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDto {
    private Long id;
    private Long assignmentId;
    private String firstName;
    private String lastName;
    private String role;
    private List<String> skills;
}
