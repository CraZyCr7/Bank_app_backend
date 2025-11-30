package com.bankapp.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fixed_deposits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedDeposit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal principal;

    @Column(nullable = false)
    private double annualInterestRate; // e.g. 6.5 for 6.5%

    @Column(nullable = false)
    private int tenureMonths; // duration in months

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate maturityDate;

    @Column(precision = 18, scale = 2)
    private BigDecimal maturityAmount; // computed at creation or on maturity

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private DepositStatus status; // ACTIVE, MATURED, RENEWED, CANCELLED

    @Column(nullable = false)
    private Boolean autoRenew = false;

    // linked account to credit maturity amount
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_account_id")
    private Account linkedAccount;

    private LocalDateTime createdAt;
    private LocalDateTime maturedAt;
}
