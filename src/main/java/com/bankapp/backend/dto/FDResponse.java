package com.bankapp.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class FDResponse {
    private Long id;
    private BigDecimal principal;
    private Double annualInterestRate;
    private Integer tenureMonths;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private BigDecimal maturityAmount;
    private String status;
    private Boolean autoRenew;
    private Long linkedAccountId;
}
