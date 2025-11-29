package com.bankapp.backend.dto;

import com.bankapp.backend.entity.Account;
import com.bankapp.backend.entity.AccountStatus;
import com.bankapp.backend.entity.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private LocalDateTime openedAt;

    private Long ownerId;
    private String ownerUsername;
}
