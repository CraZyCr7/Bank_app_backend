package com.bankapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddBeneficiaryRequest {

    @NotBlank
    @Size(min = 2, max = 120)
    private String beneficiaryName;

    @NotBlank
    @Size(min = 6, max = 50)
    private String accountNumber;

    // optional
    private String bankName;
    private String ifsc;
}
