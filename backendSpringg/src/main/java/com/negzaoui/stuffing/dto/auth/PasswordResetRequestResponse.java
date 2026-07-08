package com.negzaoui.stuffing.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetRequestResponse {

    /**
     * Message utilisateur (ne divulgue pas si l'email existe).
     */
    private String message;

    /**
     * Token de reset (présent uniquement en mode PFE/dev pour faciliter les tests).
     */
    private String token;
}

