package com.bankapp.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebitCardSpendRequest {

    @NotNull
    private Long cardId;

    @NotNull
    private Long fromAccountId; // account to debit (must belong to card owner)

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    private String merchant; // optional

    private Boolean international; // optional, default false

    private String narration; // optional
}
