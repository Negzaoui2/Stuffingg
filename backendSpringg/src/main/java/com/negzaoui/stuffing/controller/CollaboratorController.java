package com.negzaoui.stuffing.controller;

import com.negzaoui.stuffing.dto.collaborator.*;
import com.negzaoui.stuffing.dto.manager.CalendarEventDto;
import com.negzaoui.stuffing.dto.manager.PageResponseDto;
import com.negzaoui.stuffing.dto.MessageResponse;
import com.negzaoui.stuffing.security.KeycloakHelper;
import com.negzaoui.stuffing.service.CollaboratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Collaborateur", description = "Espace Collaborateur : dashboard, assignments, planning, conges, profil")
@RestController
@RequestMapping("/api/collaborator")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COLLABORATEUR')")
public class CollaboratorController {

    private final CollaboratorService collaboratorService;
    private final KeycloakHelper keycloakHelper;

    // ═══════════════════════════════════════════════════════════
    //  1. DASHBOARD
    // ═══════════════════════════════════════════════════════════

    @Operation(summary = "Dashboard personnalise du collaborateur connecte")
    @GetMapping("/dashboard")
    public ResponseEntity<CollaboratorDashboardDto> dashboard(Authentication auth) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.getDashboard(userId));
    }

    // ════════════════════════════════════════════════���══════════
    //  2. ASSIGNMENTS
    // ═══════════════════════════════════════════════════════════

    @Operation(summary = "Liste paginee des affectations du collaborateur")
    @GetMapping("/assignments")
    public ResponseEntity<PageResponseDto<CollaboratorAssignmentDetailDto>> assignments(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.getAssignments(userId, status, page, size));
    }

    @Operation(summary = "Detail complet d'une affectation")
    @GetMapping("/assignments/{id}")
    public ResponseEntity<CollaboratorAssignmentDetailDto> assignmentDetail(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.getAssignmentDetail(userId, id));
    }

    @Operation(summary = "Marquer une affectation comme terminee (notifie le manager)")
    @PostMapping("/assignments/{id}/complete")
    public ResponseEntity<CollaboratorAssignmentDetailDto> completeAssignment(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.completeAssignment(userId, id));
    }

    // ═══════════════════════════════════════════════════════════
    //  3. CALENDAR (FullCalendar)
    // ═══════════════════════════════════════════════════════════

    @Operation(summary = "Evenements calendrier du collaborateur (assignments + conges)")
    @GetMapping("/calendar/events")
    public ResponseEntity<List<CalendarEventDto>> calendarEvents(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.getCalendarEvents(userId, start, end));
    }

    // ═══════════════════════════════════════════════════════════
    //  4. CONGES & ABSENCES
    // ═══════════════════════════════════════════════════════════

    @Operation(summary = "Liste paginee des demandes de conge")
    @GetMapping("/leaves")
    public ResponseEntity<PageResponseDto<LeaveRequestDto>> leaves(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.getLeaves(userId, status, page, size));
    }

    @Operation(summary = "Creer une demande de conge")
    @PostMapping("/leaves")
    public ResponseEntity<LeaveRequestDto> createLeave(
            Authentication auth,
            @Valid @RequestBody CreateLeaveRequest request
    ) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.createLeave(userId, request));
    }

    @Operation(summary = "Annuler une demande de conge en attente")
    @DeleteMapping("/leaves/{id}")
    public ResponseEntity<MessageResponse> cancelLeave(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        collaboratorService.cancelLeave(userId, id);
        return ResponseEntity.ok(MessageResponse.builder().message("Demande de conge annulee").build());
    }

    @Operation(summary = "Solde de conges du collaborateur")
    @GetMapping("/leaves/balance")
    public ResponseEntity<LeaveBalanceDto> leaveBalance(Authentication auth) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.getLeaveBalance(userId));
    }

    // ═══════════════════════════════════════════════════════════
    //  5. PROFIL & COMPETENCES
    // ═══════════════════════════════════════════════════════════

    @Operation(summary = "Profil complet du collaborateur connecte")
    @GetMapping("/profile")
    public ResponseEntity<CollaboratorProfileDto> profile(Authentication auth) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.getProfile(userId, true));
    }

    @Operation(summary = "Mettre a jour le profil (telephone)")
    @PutMapping("/profile")
    public ResponseEntity<CollaboratorProfileDto> updateProfile(
            Authentication auth,
            @RequestBody UpdateProfileRequest request
    ) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.updateProfile(userId, request));
    }

    @Operation(summary = "Mettre a jour les competences du collaborateur")
    @PutMapping("/profile/skills")
    public ResponseEntity<CollaboratorProfileDto> updateSkills(
            Authentication auth,
            @Valid @RequestBody UpdateSkillsRequest request
    ) {
        Long userId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(collaboratorService.updateSkills(userId, request));
    }

    @Operation(summary = "Suggestions de noms de competences (autocomplete)")
    @GetMapping("/skills/suggestions")
    public ResponseEntity<List<String>> skillSuggestions(
            @RequestParam String query
    ) {
        return ResponseEntity.ok(collaboratorService.suggestSkills(query));
    }
}

