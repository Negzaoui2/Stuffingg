package com.negzaoui.stuffing.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateDepartementRequest {
    @NotBlank(message = "Le nom du département est obligatoire")
    @Size(min = 2, max = 50)
    private String name;
}

