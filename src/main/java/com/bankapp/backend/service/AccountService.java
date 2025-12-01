package com.bankapp.backend.service;

import com.bankapp.backend.dto.AccountResponse;
import com.bankapp.backend.dto.DepositRequest;
import com.bankapp.backend.dto.DepositResponse;
import com.bankapp.backend.dto.OpenAccountRequest;
import com.bankapp.backend.entity.*;
import com.bankapp.backend.repository.AccountRepository;
import com.bankapp.backend.repository.FailedTransactionRepository;
import com.bankapp.backend.repository.TransactionRecordRepository;
import com.bankapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    private final TransactionRecordRepository txRepo;
    private final FailedTransactionRepository failedRepo;

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

    @Transactional
    public DepositResponse depositToAccount(DepositRequest req, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.getAmount() == null || req.getAmount().doubleValue() <= 0) {
            throw new RuntimeException("Invalid deposit amount");
        }

        // Resolve & lock the account row to avoid races.
        // Prefer account number if provided; otherwise use id.
        Account toAccount;
        if (req.getToAccountNumber() != null && !req.getToAccountNumber().isBlank()) {
            // requires AccountRepository.findByAccountNumberForUpdate(...)
            var toAccountOpt = accountRepository.findByAccountNumberForUpdate(req.getToAccountNumber());
            toAccount = toAccountOpt.orElseThrow(() -> new RuntimeException("Destination account not found by account number"));
        } else {
            var toAccountOpt = accountRepository.findByIdForUpdate(req.getToAccountId());
            toAccount = toAccountOpt.orElseThrow(() -> new RuntimeException("Destination account not found"));
        }

        // Ensure the account belongs to the authenticated user
        if (!toAccount.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: you can only deposit to your own accounts");
        }

        // Generate reference
        String reference = generateReference("DEPOSIT");

        // Create transaction record (PENDING)
        TransactionRecord tx = TransactionRecord.builder()
                .reference(reference)
                .type(TransactionType.DEPOSIT) // or create new type DEPOSIT; see note below
                .status(TransactionStatus.PENDING)
                .toAccount(toAccount)
                .amount(req.getAmount())
                .narration(req.getNarration() != null ? req.getNarration() : ("Deposit: " + (req.getSource() == null ? "CASH" : req.getSource())))
                .beneficiaryName(req.getSource())
                .createdAt(LocalDateTime.now())
                .build();

        tx = txRepo.save(tx);

        try {
            // Credit the account
            BigDecimal before = toAccount.getBalance() == null ? BigDecimal.ZERO : toAccount.getBalance();
            toAccount.setBalance(before.add(req.getAmount()));
            accountRepository.save(toAccount);

            // mark tx success
            tx.setStatus(TransactionStatus.SUCCESS);
            tx.setProcessedAt(LocalDateTime.now());
            txRepo.save(tx);

            return DepositResponse.builder()
                    .reference(reference)
                    .status(tx.getStatus().name())
                    .toAccountId(toAccount.getId())
                    .amount(req.getAmount())
                    .newBalance(toAccount.getBalance())
                    .processedAt(tx.getProcessedAt())
                    .narration(tx.getNarration())
                    .build();

        } catch (Exception ex) {
            tx.setStatus(TransactionStatus.FAILED);
            txRepo.save(tx);
            failedRepo.save(FailedTransaction.builder()
                    .reference(reference)
                    .reason("Deposit failed: " + ex.getMessage())
                    .build());
            throw ex;
        }
    }

    private String generateReference(String prefix) {
        String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        return prefix + "-" + date + "-" + shortId;
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
