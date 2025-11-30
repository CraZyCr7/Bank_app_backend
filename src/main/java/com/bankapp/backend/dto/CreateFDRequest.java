package com.bankapp.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateFDRequest {
    @NotNull
    @DecimalMin("100.00")
    private BigDecimal principal;

    @NotNull
    @DecimalMin("0.01")
    private Double annualInterestRate; // in percent, e.g. 6.5

    @NotNull
    @Min(1)
    private Integer tenureMonths;

    private Long linkedAccountId; // where maturity credited
    private Boolean autoRenew = false;
}
