package com.bankapp.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {


    private Long fromAccountId;

    // toAccountId may be null for external transfers (we use beneficiaryAccountNumber/IFSC)
    private Long toAccountId;

    private String fromAccountNumber;
    private String toAccountNumber;

    private String beneficiaryAccountNumber;
    private String beneficiaryIfsc;
    private String beneficiaryName;

    @NotNull
    @DecimalMin(value = "1.00")
    private BigDecimal amount;

    @Size(max = 200)
    private String narration;

    // Transfer type: IMPS or NEFT
    @NotBlank
    private String type;
}
