package com.bankapp.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CardSpendResponse {
    private String reference;
    private String status;
    private BigDecimal amount;
    private String merchant;
    private Long fromAccountId;
    private BigDecimal remainingBalance;
    private LocalDateTime processedAt;
}
