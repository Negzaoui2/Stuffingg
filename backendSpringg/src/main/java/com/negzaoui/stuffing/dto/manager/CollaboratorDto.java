package com.negzaoui.stuffing.dto.manager;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String department;
    private List<String> skills;
    private String availability;
    private String currentProject;
    private String availableFrom;
    private List<CollaboratorAssignmentDto> assignments;
}

