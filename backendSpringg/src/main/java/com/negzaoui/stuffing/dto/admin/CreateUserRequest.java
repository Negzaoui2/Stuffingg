package com.negzaoui.stuffing.dto.admin;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import com.negzaoui.stuffing.entity.Role;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CreateUserRequest {

    @NotNull(message = "Le rôle est obligatoire")
    private Role role;

    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;


    @Email(message = "Format d'email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;


    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;


    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

}




