package com.negzaoui.stuffing.controller;

import com.negzaoui.stuffing.dto.manager.*;
import com.negzaoui.stuffing.security.KeycloakHelper;
import com.negzaoui.stuffing.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Manager - Dashboard & Equipe", description = "Endpoints DELIVERY_MANAGER : dashboard, team, projets")
@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY_MANAGER')")
public class ManagerController {

    private final ManagerService managerService;
    private final KeycloakHelper keycloakHelper;

    // ─── Dashboard ────────────────────────────────────────────

    @Operation(summary = "KPIs du tableau de bord du manager")
    @GetMapping("/dashboard")
    public ResponseEntity<ManagerDashboardDto> dashboard(Authentication auth) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(managerService.getDashboard(managerId));
    }

    // ─── Team ─────────────────────────────────────────────────

    @Operation(summary = "Liste paginee des collaborateurs de l'equipe")
    @GetMapping("/team")
    public ResponseEntity<PageResponseDto<CollaboratorDto>> team(
            Authentication auth,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String availability,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(managerService.getTeam(managerId, search, skill, availability, page, size));
    }

    @Operation(summary = "Detail complet d'un collaborateur")
    @GetMapping("/team/{id}")
    public ResponseEntity<CollaboratorDto> teamMember(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(managerService.getCollaboratorDetail(managerId, id));
    }

    // ─── Projects ─────────────────────────────────────────────

    @Operation(summary = "Liste paginee des projets geres par le manager")
    @GetMapping("/projects")
    public ResponseEntity<PageResponseDto<ProjectDto>> projects(
            Authentication auth,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(managerService.getProjects(managerId, search, status, page, size));
    }

    @Operation(summary = "Detail complet d'un projet")
    @GetMapping("/projects/{id}")
    public ResponseEntity<ProjectDto> projectDetail(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(managerService.getProjectDetail(managerId, id));
    }

    // ─── Calendar (FullCalendar) ──────────────────────────────

    @Operation(summary = "Événements calendrier (assignments + projets) pour FullCalendar")
    @GetMapping("/calendar/events")
    public ResponseEntity<List<CalendarEventDto>> calendarEvents(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(managerService.getCalendarEvents(managerId, start, end));
    }

    @Operation(summary = "Ressources calendrier (collaborateurs) pour la vue Timeline FullCalendar")
    @GetMapping("/calendar/resources")
    public ResponseEntity<List<CalendarResourceDto>> calendarResources(Authentication auth) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(managerService.getCalendarResources(managerId));
    }

    // ─── ACTIONS : Créer projet, Assigner, Désassigner ───────

    @Operation(summary = "Créer un nouveau projet")
    @PostMapping("/projects")
    public ResponseEntity<ProjectDto> createProject(Authentication auth, @Valid @RequestBody CreateProjectRequest req) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.createProject(managerId, req));
    }

    @Operation(summary = "Modifier un projet existant")
    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectDto> updateProject(Authentication auth, @PathVariable Long id, @Valid @RequestBody CreateProjectRequest req) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(managerService.updateProject(managerId, id, req));
    }

    @Operation(summary = "Changer le statut d'un projet (ACTIVE, COMPLETED, ON_HOLD, PLANNED)")
    @PatchMapping("/projects/{id}/status")
    public ResponseEntity<Void> updateProjectStatus(Authentication auth, @PathVariable Long id, @RequestParam String status) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        managerService.updateProjectStatus(managerId, id, status);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer (archiver) un projet : statut ARCHIVED + cloture des assignments actifs")
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(Authentication auth, @PathVariable Long id) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        managerService.archiveProject(managerId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Assigner un collaborateur à un projet")
    @PostMapping("/projects/{projectId}/assignments")
    public ResponseEntity<Void> assignCollaborator(Authentication auth, @PathVariable Long projectId, @Valid @RequestBody AssignCollaboratorRequest req) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        managerService.assignCollaborator(managerId, projectId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Retirer un collaborateur d'un projet (marque l'assignment comme COMPLETED)")
    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<Void> unassignCollaborator(Authentication auth, @PathVariable Long assignmentId) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        managerService.unassignCollaborator(managerId, assignmentId);
        return ResponseEntity.noContent().build();
    }

    // ─── Congés de l'équipe ──────────────────────────────────

    @Operation(summary = "Liste paginée des demandes de congé de l'équipe")
    @GetMapping("/leaves")
    public ResponseEntity<PageResponseDto<LeaveRequestManagerDto>> teamLeaves(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        return ResponseEntity.ok(managerService.getTeamLeaves(managerId, status, page, size));
    }

    @Operation(summary = "Approuver une demande de congé")
    @PatchMapping("/leaves/{leaveId}/approve")
    public ResponseEntity<Void> approveLeave(Authentication auth, @PathVariable Long leaveId) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        managerService.approveLeave(managerId, leaveId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rejeter une demande de congé")
    @PatchMapping("/leaves/{leaveId}/reject")
    public ResponseEntity<Void> rejectLeave(Authentication auth, @PathVariable Long leaveId) {
        Long managerId = keycloakHelper.getCurrentUserId(auth);
        managerService.rejectLeave(managerId, leaveId);
        return ResponseEntity.noContent().build();
    }
}

