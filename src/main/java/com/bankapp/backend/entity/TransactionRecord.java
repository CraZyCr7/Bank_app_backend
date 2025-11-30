package com.bankapp.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Length;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique reference
    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    //Beneficiary details (snapshot)
    @Column(length = 120)
    private String beneficiaryName;

    @Column(length = 30)
    private String beneficiaryAccountNumber;

    @Column(length = 64)
    private String beneficiaryIfsc;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 200)
    private String narration;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
