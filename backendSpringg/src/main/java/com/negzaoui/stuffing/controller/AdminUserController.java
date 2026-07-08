package com.negzaoui.stuffing.controller;

import com.negzaoui.stuffing.dto.MessageResponse;
import com.negzaoui.stuffing.dto.admin.*;
import com.negzaoui.stuffing.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Gestion des utilisateurs", description = "Endpoints ADMIN pour gérer les users")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Lister les utilisateurs (paginé, avec filtres)")
    @GetMapping
    public ResponseEntity<UserPageResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminUserService.searchUsers(search, role, isActive, page, size));
    }

    @Operation(summary = "Obtenir le détail d'un utilisateur")
    @GetMapping("/{id}")
    public ResponseEntity<UserAdminDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @Operation(summary = "Créer un utilisateur avec un rôle (ADMIN, DELIVERY_MANAGER, COLLABORATEUR)")
    @PostMapping
    public ResponseEntity<UserAdminDto> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(adminUserService.createUser(req));
    }

    @Operation(summary = "Activer / Désactiver un utilisateur")
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ToggleStatusResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.toggleStatus(id));
    }

    @Operation(summary = "Supprimer un utilisateur")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok(MessageResponse.builder().message("Utilisateur supprimé").build());
    }

    @Operation(summary = "Réinitialiser le mot de passe d'un utilisateur (par ID)")
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest req
    ) {
        adminUserService.resetPassword(id, req.getNewPassword());
        return ResponseEntity.ok(MessageResponse.builder().message("Mot de passe réinitialisé").build());
    }
}
