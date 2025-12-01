package com.bankapp.backend.service;

import com.bankapp.backend.dto.TransferRequest;
import com.bankapp.backend.dto.TransferResponse;
import com.bankapp.backend.entity.*;
import com.bankapp.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRecordRepository txRepo;
    private final FailedTransactionRepository failedRepo;
    private final BeneficiaryRepository beneficiaryRepository; // optional, if you have it

    // Example transfer limits (can be loaded from config)
    private final BigDecimal SINGLE_TRANSFER_LIMIT = BigDecimal.valueOf(1000000); // e.g. 1,000,000
    private final BigDecimal DAILY_LIMIT = BigDecimal.valueOf(2000000);

    /**
     * IMPS = immediate; do it atomically using DB lock
     */
    // src/main/java/com/bankapp/backend/service/TransferService.java
    @Transactional
    public TransferResponse doImps(TransferRequest req, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Resolve "from" account (prefer accountNumber, fallback to id)
        Account fromAccount;
        if (req.getFromAccountNumber() != null && !req.getFromAccountNumber().isBlank()) {
            fromAccount = accountRepository.findByAccountNumberForUpdate(req.getFromAccountNumber())
                    .orElseThrow(() -> new RuntimeException("Source account not found by accountNumber"));
        } else if (req.getFromAccountId() != null) {
            fromAccount = accountRepository.findByIdForUpdate(req.getFromAccountId())
                    .orElseThrow(() -> new RuntimeException("Source account not found by id"));
        } else {
            throw new RuntimeException("Either fromAccountId or fromAccountNumber must be provided");
        }

        // Ownership check
        if (!fromAccount.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: not owner of source account");
        }

        // Resolve "to" account (internal destination) — optional
        Account toAccount = null;
        if (req.getToAccountNumber() != null && !req.getToAccountNumber().isBlank()) {
            // try find and lock destination account (internal transfer)
            toAccount = accountRepository.findByAccountNumberForUpdate(req.getToAccountNumber()).orElse(null);
        } else if (req.getToAccountId() != null) {
            toAccount = accountRepository.findByIdForUpdate(req.getToAccountId()).orElse(null);
        } // else external beneficiary (we will keep beneficiaryAccountNumber)

        // Validate amount & limits (same as before)
        if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid amount");
        }
        if (req.getAmount().compareTo(SINGLE_TRANSFER_LIMIT) > 0) {
            throw new RuntimeException("Exceeds single transfer limit");
        }

        // Now perform debit/credit like before (same logic)
        String reference = generateReference(req.getType());
        TransactionRecord tx = TransactionRecord.builder()
                .reference(reference)
                .type(TransactionType.IMPS)
                .status(TransactionStatus.PENDING)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .beneficiaryName(req.getBeneficiaryName())
                .beneficiaryAccountNumber(req.getToAccountNumber() != null ? req.getToAccountNumber() : req.getBeneficiaryAccountNumber())
                .beneficiaryIfsc(req.getBeneficiaryIfsc())
                .amount(req.getAmount())
                .narration(req.getNarration())
                .createdAt(LocalDateTime.now())
                .build();
        tx = txRepo.save(tx);

        try {
            if (fromAccount.getBalance().compareTo(req.getAmount()) < 0) {
                tx.setStatus(TransactionStatus.FAILED);
                txRepo.save(tx);
                failedRepo.save(FailedTransaction.builder()
                        .reference(reference)
                        .reason("Insufficient balance")
                        .build());
                throw new RuntimeException("Insufficient balance");
            }

            // Debit
            fromAccount.setBalance(fromAccount.getBalance().subtract(req.getAmount()));
            accountRepository.save(fromAccount);

            // Credit if internal (toAccount != null)
            if (toAccount != null) {
                toAccount.setBalance(toAccount.getBalance().add(req.getAmount()));
                accountRepository.save(toAccount);
            } else {
                // external: integrate with switch/gateway
            }

            tx.setStatus(TransactionStatus.SUCCESS);
            tx.setProcessedAt(LocalDateTime.now());
            txRepo.save(tx);

            return TransferResponse.builder()
                    .reference(reference)
                    .status(tx.getStatus())
                    .amount(tx.getAmount())
                    .createdAt(tx.getCreatedAt())
                    .processedAt(tx.getProcessedAt())
                    .build();

        } catch (Exception ex) {
            tx.setStatus(TransactionStatus.FAILED);
            txRepo.save(tx);
            failedRepo.save(FailedTransaction.builder()
                    .reference(reference)
                    .reason(ex.getMessage())
                    .build());
            throw ex;
        }
    }


    /**
     * NEFT = create pending transaction (settlement later)
     */
    @Transactional
    public TransferResponse createNeft(TransferRequest req, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var fromAccount = accountRepository.findById(req.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        if (!fromAccount.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // limit checks
        if (req.getAmount().compareTo(SINGLE_TRANSFER_LIMIT) > 0) {
            throw new RuntimeException("Exceeds single transfer limit");
        }

        String reference = generateReference(req.getType());

        TransactionRecord tx = TransactionRecord.builder()
                .reference(reference)
                .type(TransactionType.NEFT)
                .status(TransactionStatus.PENDING)
                .fromAccount(fromAccount)
                .beneficiaryName(req.getBeneficiaryName())
                .beneficiaryAccountNumber(req.getBeneficiaryAccountNumber())
                .beneficiaryIfsc(req.getBeneficiaryIfsc())
                .amount(req.getAmount())
                .narration(req.getNarration())
                .createdAt(LocalDateTime.now())
                .build();

        tx = txRepo.save(tx);

        // do NOT debit now for NEFT (depends on settlement)
        return TransferResponse.builder()
                .reference(reference)
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private String generateReference(String type) {
        // e.g. IMPS-20251130-uuidShort
        String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        return type + "-" + date + "-" + shortId;
    }

    public TransactionRecord findByReference(String reference) {
        return txRepo.findByReference(reference).orElseThrow(() -> new RuntimeException("Transaction not found"));
    }
}
