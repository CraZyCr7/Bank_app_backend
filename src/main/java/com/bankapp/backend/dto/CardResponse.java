package com.bankapp.backend.dto;

import com.bankapp.backend.entity.CardStatus;
import com.bankapp.backend.entity.CardType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CardResponse {
    private Long id;
    private String maskedCardNumber; // e.g. **** **** **** 1234
    private CardType cardType;
    private CardStatus status;
    private Boolean internationalUsageEnabled;
    private BigDecimal creditLimit;
    private BigDecimal outstandingAmount;
    private String expiry;
    private Long ownerId;
    private LocalDateTime appliedAt;
    private LocalDateTime issuedAt;
}
