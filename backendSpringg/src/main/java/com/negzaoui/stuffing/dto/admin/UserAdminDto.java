package com.negzaoui.stuffing.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    @JsonProperty("isActive")
    private boolean isActive;

    private String createdAt;
    private String lastLogin;

    // Profil
    private String phone;
    private String department;
    private List<String> skills;
    private List<AssignmentDto> assignments;
}

