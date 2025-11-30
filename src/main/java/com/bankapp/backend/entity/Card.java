package com.bankapp.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // card number (masked when returned)
    @Column(unique = true, length = 19)
    private String cardNumber;

    // store last4 for display/search, do NOT store full PAN in logs in prod
    @Column(length = 4)
    private String last4;

    @Column(length = 5) // MM/yy
    private String expiry;

    // store hashed or tokenized CVV in prod; for demo we store masked or null
    @Column(length = 3)
    private String cvvMasked;

    @Enumerated(EnumType.STRING)
    private CardType cardType; // DEBIT, CREDIT

    @Enumerated(EnumType.STRING)
    private CardStatus status; // APPLIED, ISSUED, ACTIVE, TEMP_BLOCKED, CLOSED, REJECTED

    @Column(nullable = false)
    private Boolean internationalUsageEnabled = false;

    // credit-specific
    @Column(precision = 18, scale = 2)
    private BigDecimal creditLimit; // null for debit cards

    @Column(precision = 18, scale = 2)
    private BigDecimal outstandingAmount; // for credit cards

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    @Column
    private LocalDateTime issuedAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (appliedAt == null) appliedAt = LocalDateTime.now();
        if (status == null) status = CardStatus.APPLIED;
        if (outstandingAmount == null) outstandingAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
