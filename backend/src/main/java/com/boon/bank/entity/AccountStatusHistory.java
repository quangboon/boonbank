package com.boon.bank.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "account_status_history")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class AccountStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(length = 20)
    private String oldStatus;

    @Column(nullable = false, length = 20)
    private String newStatus;

    @Column(length = 500)
    private String reason;

    @Column(length = 50)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    @PrePersist
    void onCreate() { if (changedAt == null) changedAt = OffsetDateTime.now(); }
}
