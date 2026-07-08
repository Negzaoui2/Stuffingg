package com.negzaoui.stuffing.controller;

import com.negzaoui.stuffing.entity.Notification;
import com.negzaoui.stuffing.entity.User;
import com.negzaoui.stuffing.security.KeycloakHelper;
import com.negzaoui.stuffing.service.NotificationService;
import com.negzaoui.stuffing.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notifications", description = "Gestion des notifications utilisateur")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final KeycloakHelper keycloakHelper;

    @Operation(summary = "Notifications non lues de l'utilisateur connecté")
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(Authentication authentication) {
        User currentUser = keycloakHelper.getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(currentUser));
    }

    @Operation(summary = "Toutes les notifications de l'utilisateur connecté")
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications(Authentication authentication) {
        User currentUser = keycloakHelper.getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.getAllNotifications(currentUser));
    }

    @Operation(summary = "Nombre de notifications non lues")
    @GetMapping("/count")
    public ResponseEntity<Long> countUnread(Authentication authentication) {
        User currentUser = keycloakHelper.getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.countUnread(currentUser));
    }

    @Operation(summary = "Marquer une notification comme lue")
    @PutMapping("/{id}/read")
    public ResponseEntity<MessageResponse> markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User currentUser = keycloakHelper.getCurrentUser(authentication);
        notificationService.markAsRead(id, currentUser);
        return ResponseEntity.ok(MessageResponse.builder().message("Notification marquée comme lue").build());
    }

    @Operation(summary = "Marquer toutes les notifications comme lues")
    @PutMapping("/read-all")
    public ResponseEntity<MessageResponse> markAllAsRead(Authentication authentication) {
        User currentUser = keycloakHelper.getCurrentUser(authentication);
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok(MessageResponse.builder().message("Toutes les notifications marquées comme lues").build());
    }
}