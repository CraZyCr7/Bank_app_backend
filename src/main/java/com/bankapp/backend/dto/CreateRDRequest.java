package com.bankapp.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRDRequest {
    @NotNull
    @DecimalMin("100.00")
    private BigDecimal monthlyInstallment;

    @NotNull
    @DecimalMin("0.01")
    private Double annualInterestRate;

    @NotNull
    @Min(1)
    private Integer tenureMonths;

    private Long linkedAccountId;
}
