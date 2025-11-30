package com.bankapp.backend.dto;

import com.bankapp.backend.entity.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransferResponse {
    private String reference;
    private TransactionStatus status;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
