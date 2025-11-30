package com.bankapp.backend.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class PayCardBillRequest {
    @NotNull
    private Long cardId;

    @NotNull
    private Long fromAccountId; // debiting this account to pay the bill

    @NotNull
    @DecimalMin("1.0")
    private BigDecimal amount;
}
