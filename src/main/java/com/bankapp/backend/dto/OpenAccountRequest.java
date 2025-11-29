package com.bankapp.backend.dto;

import com.bankapp.backend.entity.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OpenAccountRequest {

    @NotNull
    private AccountType accountType;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal initialDeposit;
}
