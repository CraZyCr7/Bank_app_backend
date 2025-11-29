package com.bankapp.backend.service;

import com.bankapp.backend.dto.AccountResponse;
import com.bankapp.backend.dto.OpenAccountRequest;
import com.bankapp.backend.entity.*;
import com.bankapp.backend.repository.AccountRepository;
import com.bankapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public AccountResponse openAccount(OpenAccountRequest request, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // business rule: only one account per type per user
        if (accountRepository.existsByOwnerAndAccountType(user, request.getAccountType())) {
            throw new RuntimeException(
                    "You already have a " + request.getAccountType() + " account");
        }
        // 1️⃣ FIRST SAVE (accountNumber is null)
        var account = Account.builder()
                .owner(user)
                .accountType(request.getAccountType())
                .balance(request.getInitialDeposit())
                .status(AccountStatus.ACTIVE)
                .build();

        account = accountRepository.save(account); // INSERT → gets ID here

        // 2️⃣ Now generate correct number using ID
        String accountNumber = generateAccountNumber(
                account.getAccountType(),
                account.getId()
        );
        account.setAccountNumber(accountNumber);

        // 3️⃣ SECOND SAVE (update accountNumber)
        account = accountRepository.save(account);

        return toResponse(account);
    }


    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        var accounts = accountRepository.findByOwner(user);
        return accounts.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountDetails(Long id, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        var account = accountRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new RuntimeException("Account not found or not owned by user"));

        return toResponse(account);
    }

    private String generateAccountNumber(AccountType type, Long id) {
        String prefix = switch (type) {
            case SAVINGS -> "SB";
            case CURRENT -> "CA";
        };
        String datePart = java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);// e.g. 20251129
        return "%s-%s-%06d".formatted(prefix, datePart, id);
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .balance(account.getBalance())
                .openedAt(account.getOpenedAt())
                .ownerId(account.getOwner().getId())
                .ownerUsername(account.getOwner().getUsername())
                .build();
    }

}
