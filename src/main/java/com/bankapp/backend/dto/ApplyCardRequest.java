package com.bankapp.backend.dto;

import com.bankapp.backend.entity.CardType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplyCardRequest {
    @NotNull
    private CardType cardType; // DEBIT or CREDIT

    // for credit card applications
    private BigDecimal requestedCreditLimit; // optional for credit

    // optionally select linked accountId for debit/auto-debit for credit bills
    private Long linkedAccountId;
}
