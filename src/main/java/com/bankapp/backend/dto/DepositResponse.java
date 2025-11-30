package com.bankapp.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DepositResponse {
    private String reference;
    private String status; // SUCCESS / FAILED
    private Long toAccountId;
    private BigDecimal amount;
    private BigDecimal newBalance;
    private LocalDateTime processedAt;
    private String narration;
}
