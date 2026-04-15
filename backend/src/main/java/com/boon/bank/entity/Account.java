package com.boon.bank.entity;

import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.exception.BusinessException;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.InsufficientFundsException;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "account")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal transactionLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime openedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Version
    private Integer version;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (openedAt == null) openedAt = OffsetDateTime.now();
        if (status == null) status = AccountStatus.ACTIVE;
        if (balance == null) balance = BigDecimal.ZERO;
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public void checkActive() {
        if (status != AccountStatus.ACTIVE)
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE,
                    "Account " + accountNumber + " is " + status);
    }

    public void debit(BigDecimal amt) {
        if (balance.compareTo(amt) < 0)
            throw new InsufficientFundsException("Not enough balance");
        this.balance = balance.subtract(amt);
    }

    public void credit(BigDecimal amt) {
        this.balance = balance.add(amt);
    }

    public void checkLimit(BigDecimal amt) {
        if (amt.compareTo(transactionLimit) > 0)
            throw new BusinessException(ErrorCode.LIMIT_EXCEEDED,
                    "Amount " + amt + " exceeds limit " + transactionLimit);
    }
}
