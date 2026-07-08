package com.negzaoui.stuffing.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import com.negzaoui.stuffing.service.AccountCreationRequestService;
import com.negzaoui.stuffing.dto.MessageResponse;
import com.negzaoui.stuffing.dto.auth.AccountCreationRequestDto;

@RequiredArgsConstructor
@RequestMapping("/api/public")
@RestController
public class AccountCreationRequestController {
    private final AccountCreationRequestService service;

    @PostMapping("/account-requests")
    @Operation(
            summary = "Demande de création de compte",
            description = "Endpoint public: soumet une demande, traitée ensuite par l'Admin/Delivery Manager."
    )
    public ResponseEntity<MessageResponse> submit(@Valid @RequestBody AccountCreationRequestDto dto) {
        service.submit(dto);
        return ResponseEntity.ok(
                MessageResponse.builder().message("Votre demande a été envoyée à l'administrateur").build()
        );
    }
}
