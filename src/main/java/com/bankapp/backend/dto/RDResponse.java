package com.bankapp.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RDResponse {
    private Long id;
    private BigDecimal monthlyInstallment;
    private Double annualInterestRate;
    private Integer tenureMonths;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private BigDecimal maturityAmount;
    private String status;
    private Long linkedAccountId;
}
