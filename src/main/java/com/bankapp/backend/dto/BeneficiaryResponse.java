package com.bankapp.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BeneficiaryResponse {
    private Long id;
    private String beneficiaryName;
    private String accountNumber;
    private String bankName;
    private String ifsc;
    private boolean internal;
    private Long beneficiaryUserId; // if internal
    private Long ownerId;
    private String ownerUsername;
    private LocalDateTime createdAt;
}
