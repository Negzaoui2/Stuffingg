package com.negzaoui.stuffing.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    /** Type de notification : ACCOUNT_REQUEST, ACCOUNT_APPROVED, ACCOUNT_REJECTED, etc. */
    @Column(nullable = false)
    private String type;

    /** Affectation liée (optionnel) — sert notamment à éviter les notifications en double côté scheduler. */
    @Column(name = "assignment_id")
    private Long assignmentId;

    /** Utilisateur cible de la notification (ex: l'ADMIN qui doit voir la demande). Null = broadcast. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_user_id")
    @JsonIgnoreProperties({"password", "profile"})
    private User targetUser;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}
