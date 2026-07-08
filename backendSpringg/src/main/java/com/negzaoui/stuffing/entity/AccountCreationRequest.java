package com.negzaoui.stuffing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account_creation_requests")
public class AccountCreationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String email;

    @Column
    private String phone;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String jobTitle;

    @Column(length = 4000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountRequestStatus status = AccountRequestStatus.PENDING;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column
    private Instant processedAt;

    @Column
    private String processedBy;
}

