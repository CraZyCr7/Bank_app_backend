package com.bankapp.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {

    private Long toAccountId;
    private String toAccountNumber;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    /**
     * optional source description: "CASH", "CHEQUE", "UPI", "NEFT from X", etc.
     */
    //private String source;

    private String narration;

}
