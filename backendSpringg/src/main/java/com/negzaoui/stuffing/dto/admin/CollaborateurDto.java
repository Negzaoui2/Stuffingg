package com.negzaoui.stuffing.dto.admin;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaborateurDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;       // ex: "COLLABORATEUR", "DELIVERY_MANAGER"
    private boolean isActive;
}

