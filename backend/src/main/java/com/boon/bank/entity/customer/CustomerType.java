package com.boon.bank.entity.customer;

import com.boon.bank.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "single_txn_limit", precision = 19, scale = 2)
    private BigDecimal singleTxnLimit;

    @Column(name = "daily_txn_limit", precision = 19, scale = 2)
    private BigDecimal dailyTxnLimit;

    @Column(name = "monthly_txn_limit", precision = 19, scale = 2)
    private BigDecimal monthlyTxnLimit;

    @Column(name = "fee_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal feeRate = BigDecimal.ZERO;
}
