package com.negzaoui.stuffing.controller;

import com.negzaoui.stuffing.dto.MessageResponse;
import com.negzaoui.stuffing.dto.admin.CollaborateurDto;
import com.negzaoui.stuffing.dto.admin.CreateDepartementRequest;
import com.negzaoui.stuffing.dto.admin.DepartementDto;
import com.negzaoui.stuffing.service.DepartementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - Gestion des départements", description = "CRUD des départements (réservé ADMIN)")
@RestController
@RequestMapping("/api/admin/departements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartementController {

    private final DepartementService departementService;

    @Operation(summary = "Lister tous les départements")
    @GetMapping
    public ResponseEntity<List<DepartementDto>> getAll() {
        return ResponseEntity.ok(departementService.getAllDepartements());
    }

    @Operation(summary = "Obtenir un département par ID")
    @GetMapping("/{id}")
    public ResponseEntity<DepartementDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(departementService.getDepartementById(id));
    }

    @Operation(summary = "Lister les collaborateurs rattachés à un département")
    @GetMapping("/{id}/collaborateurs")
    public ResponseEntity<List<CollaborateurDto>> getCollaborateurs(@PathVariable Long id) {
        return ResponseEntity.ok(departementService.getCollaborateursByDepartement(id));
    }

    @Operation(summary = "Créer un nouveau département")
    @PostMapping
    public ResponseEntity<DepartementDto> create(@Valid @RequestBody CreateDepartementRequest request) {
        return ResponseEntity.ok(departementService.createDepartement(request));
    }

    @Operation(summary = "Modifier un département")
    @PutMapping("/{id}")
    public ResponseEntity<DepartementDto> update(@PathVariable Long id,
                                                  @Valid @RequestBody CreateDepartementRequest request) {
        return ResponseEntity.ok(departementService.updateDepartement(id, request));
    }

    @Operation(summary = "Supprimer un département (uniquement s'il est vide)")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        departementService.deleteDepartement(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Département supprimé avec succès")
                .build());
    }
}

