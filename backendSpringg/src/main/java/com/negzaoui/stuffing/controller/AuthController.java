package com.negzaoui.stuffing.controller;

import com.negzaoui.stuffing.dto.*;
import com.negzaoui.stuffing.dto.auth.*;
import com.negzaoui.stuffing.entity.User;
import com.negzaoui.stuffing.security.KeycloakHelper;
import com.negzaoui.stuffing.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService passwordResetService;
    private final KeycloakHelper keycloakHelper;

    // ─── /me : retourne les infos de l'utilisateur connecté (depuis le token Keycloak) ───

    @Operation(
            summary = "Informations utilisateur connecté",
            description = "Retourne les informations de l'utilisateur à partir du token Keycloak. Remplace l'ancien /login qui retournait un token."
    )
    @ApiResponse(responseCode = "200", description = "Utilisateur trouvé",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class)))
    @GetMapping("/me")
    public ResponseEntity<AuthenticationResponse> me(Authentication authentication) {
        User user = keycloakHelper.getCurrentUser(authentication);
        return ResponseEntity.ok(AuthenticationResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build());
    }

    // ─── Reset password (toujours utile côté serveur) ───

    @Operation(
            summary = "Demande de réinitialisation de mot de passe",
            description = "Génère un token de réinitialisation (en mode PFE/dev, le token est retourné dans la réponse)"
    )
    @ApiResponse(responseCode = "200", description = "Demande traitée",
            content = @Content(schema = @Schema(implementation = PasswordResetRequestResponse.class)))
    @PostMapping("/reset-password/request")
    public ResponseEntity<PasswordResetRequestResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        String token = passwordResetService.createResetToken(request.getEmail());

        var response = PasswordResetRequestResponse.builder()
                .message("Si l'email existe, un lien de réinitialisation a été généré")
                .token(token)
                .build();

        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Confirmation de réinitialisation de mot de passe",
            description = "Valide le token et met à jour le mot de passe"
    )
    @ApiResponse(responseCode = "200", description = "Mot de passe mis à jour",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
    @PostMapping("/reset-password/confirm")
    public ResponseEntity<MessageResponse> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        passwordResetService.confirmReset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(MessageResponse.builder().message("Mot de passe mis à jour").build());
    }
}
