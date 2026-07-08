package com.negzaoui.stuffing.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RejectAccountCreationRequest {

    /**
     * Optionnel : raison du rejet (sera envoyée dans l'email si configuré).
     */
    private String reason;
}

