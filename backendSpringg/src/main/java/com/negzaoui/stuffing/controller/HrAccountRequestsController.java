package com.negzaoui.stuffing.controller;

import com.negzaoui.stuffing.dto.MessageResponse;
import com.negzaoui.stuffing.dto.auth.AccountRequestStatsDto;
import com.negzaoui.stuffing.dto.auth.ApproveAccountCreationRequest;
import com.negzaoui.stuffing.dto.auth.RejectAccountCreationRequest;
import com.negzaoui.stuffing.entity.AccountCreationRequest;
import com.negzaoui.stuffing.entity.AccountRequestStatus;
import com.negzaoui.stuffing.entity.Role;
import com.negzaoui.stuffing.entity.User;
import com.negzaoui.stuffing.repository.UserRepository;
import com.negzaoui.stuffing.service.AccountCreationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrAccountRequestsController {

    private final AccountCreationRequestService service;
    private final UserRepository userRepository;

    @Operation(summary = "Lister les demandes (toutes ou filtrées par statut)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/account-requests")
    public ResponseEntity<Page<AccountCreationRequest>> list(
            @RequestParam(name = "status", required = false) AccountRequestStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.list(status, pageable));
    }

    @Operation(summary = "Statistiques des demandes (total, pending, approved, rejected)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/account-requests/stats")
    public ResponseEntity<AccountRequestStatsDto> stats() {
        return ResponseEntity.ok(service.stats());
    }

    @Operation(summary = "Approuver une demande et créer le compte")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/account-requests/{id}/approve")
    public ResponseEntity<MessageResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody ApproveAccountCreationRequest body,
            Authentication authentication
    ) {
        var user = service.approve(id, body.getRole(), body.getTemporaryPassword(),
                body.getManagerId(), body.getDepartementId(), authentication);
        return ResponseEntity.ok(
                MessageResponse.builder().message("Compte créé/validé pour " + user.getEmail()).build()
        );
    }

    @Operation(summary = "Rejeter une demande")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/account-requests/{id}/reject")
    public ResponseEntity<MessageResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RejectAccountCreationRequest body,
            Authentication authentication
    ) {
        String reason = (body != null) ? body.getReason() : null;
        service.reject(id, reason, authentication);
        return ResponseEntity.ok(MessageResponse.builder().message("Demande rejetée").build());
    }

    @Operation(summary = "Lister les Delivery Managers (pour le dropdown d'assignation)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/managers")
    public ResponseEntity<List<Map<String, Object>>> listManagers() {
        List<User> managers = userRepository.findByRole(Role.DELIVERY_MANAGER);
        List<Map<String, Object>> result = managers.stream()
                .filter(User::isActive)
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "firstName", m.getFirstName(),
                        "lastName", m.getLastName(),
                        "email", m.getEmail()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}