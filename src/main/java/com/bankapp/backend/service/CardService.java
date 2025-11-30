package com.bankapp.backend.service;

import com.bankapp.backend.dto.*;
import com.bankapp.backend.entity.*;
import com.bankapp.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRecordRepository txRepo;
    private final FailedTransactionRepository failedRepo;

    /* 1. Apply for card (auto-issue on apply) */
    @Transactional
    public CardResponse applyCard(ApplyCardRequest req, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Optional: check linkedAccount belongs to user
        if (req.getLinkedAccountId() != null) {
            var acct = accountRepository.findById(req.getLinkedAccountId())
                    .orElseThrow(() -> new RuntimeException("Linked account not found"));
            if (!acct.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("Linked account not owned by user");
            }
        }

        Card card = Card.builder()
                .cardType(req.getCardType())
                .owner(user)
                .creditLimit(req.getRequestedCreditLimit() == null ? BigDecimal.ZERO : req.getRequestedCreditLimit())
                .internationalUsageEnabled(false)
                .status(CardStatus.APPLIED)
                .build();

        card = cardRepository.save(card);

        // AUTO-ISSUE the card immediately (demo mode, no admin)
        autoIssueCard(card);

        // reload to ensure all fields set by autoIssueCard are present
        card = cardRepository.findById(card.getId()).orElseThrow(() -> new RuntimeException("Card not found after issue"));

        return toResponse(card);
    }

    /**
     * Auto-issue helper: sets PAN, expiry, last4, cvvMasked and marks ISSUED + issuedAt.
     * This is internal and used for demo mode when no admin exists.
     */
    private void autoIssueCard(Card card) {
        if (card == null) return;
        if (card.getStatus() != CardStatus.APPLIED) return; // only issue applied cards

        // generate cardNumber / expiry / cvv (demo)
        String pan = generateCardNumber(card.getCardType());                // formatted "#### #### #### ####"
        String panNoSpaces = pan.replaceAll("\\s+", "");
        String expiry = LocalDate.now().plusYears(4).format(DateTimeFormatter.ofPattern("MM/yy"));
        String cvvMasked = "XXX"; // demo only — do NOT store real CVV in prod

        card.setCardNumber(pan);
        // ensure last4 is extracted from numeric PAN
        card.setLast4(panNoSpaces.substring(Math.max(0, panNoSpaces.length() - 4)));
        card.setExpiry(expiry);
        card.setCvvMasked(cvvMasked);
        card.setIssuedAt(java.time.LocalDateTime.now());
        card.setStatus(CardStatus.ISSUED);

        cardRepository.save(card);
    }

    /* 2. Issue card (kept for compatibility if you ever want manual issue) */
    /*@Transactional
    public CardResponse issueCard(Long id) {
        var card = cardRepository.findById(id).orElseThrow(() -> new RuntimeException("Card not found"));
        if (card.getStatus() != CardStatus.APPLIED) {
            throw new RuntimeException("Card not in APPLIED state");
        }

        // generate cardNumber / expiry / cvv (demo)
        String pan = generateCardNumber(card.getCardType());
        String panNoSpaces = pan.replaceAll("\\s+", "");
        String expiry = LocalDate.now().plusYears(4).format(DateTimeFormatter.ofPattern("MM/yy"));
        String cvvMasked = "XXX"; // do NOT store real CVV in plain in prod
        card.setCardNumber(pan);
        card.setLast4(panNoSpaces.substring(Math.max(0, panNoSpaces.length() - 4)));
        card.setExpiry(expiry);
        card.setCvvMasked(cvvMasked);
        card.setIssuedAt(java.time.LocalDateTime.now());
        card.setStatus(CardStatus.ISSUED);

        card = cardRepository.save(card);
        return toResponse(card);
    }*/

    /* 3. Activate card (owner action to enable card) */
    @Transactional
    public CardResponse activateCard(Long cardId, String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        var card = cardRepository.findById(cardId).orElseThrow(() -> new RuntimeException("Card not found"));
        checkOwnership(card, user);
        if (card.getStatus() != CardStatus.ISSUED) throw new RuntimeException("Card not in ISSUED state");
        card.setStatus(CardStatus.ACTIVE);
        card = cardRepository.save(card);
        return toResponse(card);
    }

    /* 4. Temporary block/unblock */
    @Transactional
    public CardResponse blockCard(Long cardId, String username, String reason) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        var card = cardRepository.findById(cardId).orElseThrow(() -> new RuntimeException("Card not found"));
        checkOwnership(card, user);
        card.setStatus(CardStatus.TEMP_BLOCKED);
        // we could save reason + audit
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse unblockCard(Long cardId, String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        var card = cardRepository.findById(cardId).orElseThrow(() -> new RuntimeException("Card not found"));
        checkOwnership(card, user);
        card.setStatus(CardStatus.ACTIVE);
        return toResponse(cardRepository.save(card));
    }

    /* 5. Enable/Disable international usage */
    @Transactional
    public CardResponse setInternationalUsage(Long cardId, String username, boolean enabled) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        var card = cardRepository.findById(cardId).orElseThrow(() -> new RuntimeException("Card not found"));
        checkOwnership(card, user);
        card.setInternationalUsageEnabled(enabled);
        return toResponse(cardRepository.save(card));
    }

    /* 6. Pay credit card bill */
    @Transactional
    public CardResponse payCardBill(PayCardBillRequest req, String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        var card = cardRepository.findById(req.getCardId()).orElseThrow(() -> new RuntimeException("Card not found"));
        checkOwnership(card, user);

        if (card.getCardType() != CardType.CREDIT) throw new RuntimeException("Only credit card bills can be paid");

        var fromAccount = accountRepository.findById(req.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("From account not found"));

        if (!fromAccount.getOwner().getId().equals(user.getId())) throw new AccessDeniedException("Not owner of account");

        // Simple payment: if sufficient balance, debit and reduce outstanding
        if (fromAccount.getBalance().compareTo(req.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance to pay card bill");
        }

        // Debit account
        fromAccount.setBalance(fromAccount.getBalance().subtract(req.getAmount()));
        accountRepository.save(fromAccount);

        // Reduce outstanding amount
        if (card.getOutstandingAmount() == null) {
            card.setOutstandingAmount(BigDecimal.ZERO);
        }
        card.setOutstandingAmount(card.getOutstandingAmount().subtract(req.getAmount()));
        if (card.getOutstandingAmount().compareTo(BigDecimal.ZERO) < 0) card.setOutstandingAmount(BigDecimal.ZERO);

        card = cardRepository.save(card);

        // option: create transaction record for audit
        // txRepo.save(...)

        return toResponse(card);
    }

    /* 7. List my cards */
    @Transactional(readOnly = true)
    public List<CardResponse> listMyCards(String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return cardRepository.findByOwner(user).stream().map(this::toResponse).collect(Collectors.toList());
    }
    /* 8. addCharge Card usage*/
    @Transactional
    public CardResponse addCharge(AddChargeRequest req, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var card = cardRepository.findById(req.getCardId())
                .orElseThrow(() -> new RuntimeException("Card not found"));

        checkOwnership(card, user);

        if (card.getCardType() != CardType.CREDIT) {
            throw new RuntimeException("Charges can only be added to credit cards");
        }

        if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        // If first time setting outstandingAmount
        if (card.getOutstandingAmount() == null) {
            card.setOutstandingAmount(BigDecimal.ZERO);
        }

        // Increase outstanding
        card.setOutstandingAmount(
                card.getOutstandingAmount().add(req.getAmount())
        );

        // Optionally: limit check
        if (card.getOutstandingAmount().compareTo(card.getCreditLimit()) > 0) {
            throw new RuntimeException("Credit limit exceeded");
        }

        card = cardRepository.save(card);

        return toResponse(card);
    }
    /* Debit Card Service */
    @Transactional
    public CardSpendResponse debitCardSpend(DebitCardSpendRequest req, String username) {
        // validate user
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // load card
        var card = cardRepository.findById(req.getCardId())
                .orElseThrow(() -> new RuntimeException("Card not found"));

        // ownership check
        if (!card.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not owner of the card");
        }

        // card must be debit
        if (card.getCardType() != CardType.DEBIT) {
            throw new RuntimeException("Card is not a debit card");
        }

        // card status checks
        if (card.getStatus() != CardStatus.ACTIVE && card.getStatus() != CardStatus.ISSUED) {
            throw new RuntimeException("Card is not active");
        }
        if (card.getStatus() == CardStatus.TEMP_BLOCKED) {
            throw new RuntimeException("Card is temporarily blocked");
        }

        // international usage check
        if (Boolean.TRUE.equals(req.getInternational()) && !Boolean.TRUE.equals(card.getInternationalUsageEnabled())) {
            throw new RuntimeException("International usage is disabled on this card");
        }

        // amount validation
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        // Lock & load from account for update (use your repo method)
        var fromAccount = accountRepository.findByIdForUpdate(req.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("From account not found"));

        if (!fromAccount.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("From account does not belong to the card owner");
        }

        // Check available balance
        if (fromAccount.getBalance().compareTo(req.getAmount()) < 0) {
            // create failed tx record for audit
            String refFail = generateReference("CARD"); // reuse existing generator
            failedRepo.save(FailedTransaction.builder()
                    .reference(refFail)
                    .reason("Insufficient balance for card spend")
                    .build());
            throw new RuntimeException("Insufficient balance");
        }

        // Generate reference
        String reference = generateReference("CARD");

        // Create transaction record (PENDING -> SUCCESS in same transaction)
        TransactionRecord tx = TransactionRecord.builder()
                .reference(reference)
                .type(TransactionType.CARD) // adjust if your enum is different
                .status(TransactionStatus.PENDING)
                .fromAccount(fromAccount)
                .beneficiaryName(req.getMerchant())
                .beneficiaryAccountNumber(null)
                .beneficiaryIfsc(null)
                .amount(req.getAmount())
                .narration(req.getNarration() != null ? req.getNarration() : ("Card spend - " + req.getMerchant()))
                .createdAt(LocalDateTime.now())
                .build();

        tx = txRepo.save(tx);

        // Debit the account
        fromAccount.setBalance(fromAccount.getBalance().subtract(req.getAmount()));
        accountRepository.save(fromAccount);

        // mark tx success
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setProcessedAt(LocalDateTime.now());
        txRepo.save(tx);

        // Build response
        CardSpendResponse resp = CardSpendResponse.builder()
                .reference(reference)
                .status(tx.getStatus().name())
                .amount(tx.getAmount())
                .merchant(req.getMerchant())
                .fromAccountId(fromAccount.getId())
                .remainingBalance(fromAccount.getBalance())
                .processedAt(tx.getProcessedAt())
                .build();

        return resp;
    }

    private String generateReference(String type) {
        // e.g. IMPS-20251130-uuidShort
        String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        return type + "-" + date + "-" + shortId;
    }

    /* Helpers */
    private CardResponse toResponse(Card c) {
        String masked = c.getCardNumber() == null ? null : ("**** **** **** " + c.getLast4());
        return CardResponse.builder()
                .id(c.getId())
                .maskedCardNumber(masked)
                .cardType(c.getCardType())
                .status(c.getStatus())
                .internationalUsageEnabled(c.getInternationalUsageEnabled())
                .creditLimit(c.getCreditLimit())
                .outstandingAmount(c.getOutstandingAmount())
                .expiry(c.getExpiry())
                .ownerId(c.getOwner().getId())
                .appliedAt(c.getAppliedAt())
                .issuedAt(c.getIssuedAt())
                .build();
    }

    private void checkOwnership(Card card, User user) {
        if (!card.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not owner of card");
        }
    }

    private String generateCardNumber(CardType type) {
        // Demo: prefix by type and create 16 digits (not Luhn-checked). For demo only.
        String prefix = type == CardType.CREDIT ? "5" : "4"; // naive: 4 = visa-like, 5 = master-like
        StringBuilder pan = new StringBuilder();
        pan.append(prefix);
        while (pan.length() < 16) {
            pan.append((int)(Math.random() * 10));
        }
        // format as 4-4-4-4
        return pan.toString().replaceAll("(.{4})(?=.)", "$1 ");
    }
}
