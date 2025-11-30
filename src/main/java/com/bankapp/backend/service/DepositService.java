package com.bankapp.backend.service;

import com.bankapp.backend.dto.*;
import com.bankapp.backend.entity.*;
import com.bankapp.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepositService {

    private final FixedDepositRepository fdRepo;
    private final RecurringDepositRepository rdRepo;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRecordRepository txRepo;
    private final FailedTransactionRepository failedRepo;
    private final NotificationService notificationService; // implement a stub to send emails / events

    // ---------- FD creation ----------
    @Transactional
    public FDResponse createFD(CreateFDRequest req, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // validate linked account if present
        Account linked = null;
        if (req.getLinkedAccountId() != null) {
            linked = accountRepository.findById(req.getLinkedAccountId())
                    .orElseThrow(() -> new RuntimeException("Linked account not found"));
            if (!linked.getOwner().getId().equals(user.getId())) {
                throw new RuntimeException("Linked account not owned by user");
            }
        }

        LocalDate start = LocalDate.now();
        LocalDate maturity = start.plusMonths(req.getTenureMonths());

        BigDecimal maturityAmount = calculateFDMaturityAmount(req.getPrincipal(), req.getAnnualInterestRate(), req.getTenureMonths());

        FixedDeposit fd = FixedDeposit.builder()
                .owner(user)
                .principal(req.getPrincipal())
                .annualInterestRate(req.getAnnualInterestRate())
                .tenureMonths(req.getTenureMonths())
                .startDate(start)
                .maturityDate(maturity)
                .maturityAmount(maturityAmount)
                .status(DepositStatus.ACTIVE)
                .autoRenew(req.getAutoRenew() != null ? req.getAutoRenew() : false)
                .linkedAccount(linked)
                .createdAt(LocalDateTime.now())
                .build();

        fd = fdRepo.save(fd);

        // Optionally: create a transaction record to show deposit creation (funds moved from account to bank). Not debiting linked account here.
        notificationService.sendEmailAsync(user.getEmail(),
                "FD Created",
                "Your FD #" + fd.getId() + " has been created and will mature on " + maturity.toString());

        return toFDResponse(fd);
    }

    // ---------- RD creation ----------
    @Transactional
    public RDResponse createRD(CreateRDRequest req, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account linked = null;
        if (req.getLinkedAccountId() != null) {
            linked = accountRepository.findById(req.getLinkedAccountId())
                    .orElseThrow(() -> new RuntimeException("Linked account not found"));
            if (!linked.getOwner().getId().equals(user.getId())) {
                throw new RuntimeException("Linked account not owned by user");
            }
        }

        LocalDate start = LocalDate.now();
        LocalDate maturity = start.plusMonths(req.getTenureMonths());

        BigDecimal maturityAmount = calculateRDMaturityAmount(req.getMonthlyInstallment(), req.getAnnualInterestRate(), req.getTenureMonths());

        RecurringDeposit rd = RecurringDeposit.builder()
                .owner(user)
                .monthlyInstallment(req.getMonthlyInstallment())
                .annualInterestRate(req.getAnnualInterestRate())
                .tenureMonths(req.getTenureMonths())
                .startDate(start)
                .maturityDate(maturity)
                .maturityAmount(maturityAmount)
                .status(DepositStatus.ACTIVE)
                .linkedAccount(linked)
                .createdAt(LocalDateTime.now())
                .build();

        rd = rdRepo.save(rd);

        notificationService.sendEmailAsync(user.getEmail(),
                "RD Created",
                "Your RD #" + rd.getId() + " has been created and will mature on " + maturity.toString());

        return toRDResponse(rd);
    }

    // ---------- List / View ----------
    @Transactional(readOnly = true)
    public List<FDResponse> listMyFDs(String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return fdRepo.findByOwner(user).stream().map(this::toFDResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FDResponse getFD(Long id, String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        var fd = fdRepo.findById(id).orElseThrow(() -> new RuntimeException("FD not found"));
        if (!fd.getOwner().getId().equals(user.getId())) throw new RuntimeException("Unauthorized");
        return toFDResponse(fd);
    }

    @Transactional(readOnly = true)
    public List<RDResponse> listMyRDs(String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return rdRepo.findByOwner(user).stream().map(this::toRDResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RDResponse getRD(Long id, String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        var rd = rdRepo.findById(id).orElseThrow(() -> new RuntimeException("RD not found"));
        if (!rd.getOwner().getId().equals(user.getId())) throw new RuntimeException("Unauthorized");
        return toRDResponse(rd);
    }

    // ---------- Cancel ----------
    @Transactional
    public void cancelFD(Long id, String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        var fd = fdRepo.findById(id).orElseThrow(() -> new RuntimeException("FD not found"));
        if (!fd.getOwner().getId().equals(user.getId())) throw new RuntimeException("Unauthorized");
        if (fd.getStatus() != DepositStatus.ACTIVE) throw new RuntimeException("Only active FD can be cancelled");
        fd.setStatus(DepositStatus.CANCELLED);
        fdRepo.save(fd);
        notificationService.sendEmailAsync(user.getEmail(), "FD Cancelled", "FD #" + fd.getId() + " has been cancelled.");
    }

    @Transactional
    public void cancelRD(Long id, String username) {
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        var rd = rdRepo.findById(id).orElseThrow(() -> new RuntimeException("RD not found"));
        if (!rd.getOwner().getId().equals(user.getId())) throw new RuntimeException("Unauthorized");
        if (rd.getStatus() != DepositStatus.ACTIVE) throw new RuntimeException("Only active RD can be cancelled");
        rd.setStatus(DepositStatus.CANCELLED);
        rdRepo.save(rd);
        notificationService.sendEmailAsync(user.getEmail(), "RD Cancelled", "RD #" + rd.getId() + " has been cancelled.");
    }

    // ---------- Scheduled Maturity Processor ----------
    // Runs daily at 02:00 AM server time
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void processMaturities() {
        LocalDate today = LocalDate.now();

        // process FDs
        List<FixedDeposit> fds = fdRepo.findByMaturityDateAndStatus(today, DepositStatus.ACTIVE);
        for (FixedDeposit fd : fds) {
            try {
                handleFDMaturity(fd);
            } catch (Exception ex) {
                // log and create a failed record
                failedRepo.save(FailedTransaction.builder()
                        .reference("FDMAT-" + fd.getId())
                        .reason("FD maturity processing failed: " + ex.getMessage())
                        .build());
            }
        }

        // process RDs
        List<RecurringDeposit> rds = rdRepo.findByMaturityDateAndStatus(today, DepositStatus.ACTIVE);
        for (RecurringDeposit rd : rds) {
            try {
                handleRDMaturity(rd);
            } catch (Exception ex) {
                failedRepo.save(FailedTransaction.builder()
                        .reference("RDMAT-" + rd.getId())
                        .reason("RD maturity processing failed: " + ex.getMessage())
                        .build());
            }
        }
    }

    // ---------- Helpers for maturity ----------
    private void handleFDMaturity(FixedDeposit fd) {
        // mark matured and credit linked account (or owner's primary account if linked null)
        fd.setStatus(DepositStatus.MATURED);
        fd.setMaturedAt(LocalDateTime.now());
        fdRepo.save(fd);

        // credit maturityAmount to linked account if present, else skip and notify
        if (fd.getLinkedAccount() != null) {
            accountRepository.findByIdForUpdate(fd.getLinkedAccount().getId()).ifPresent(acc -> {
                acc.setBalance(acc.getBalance().add(fd.getMaturityAmount()));
                accountRepository.save(acc);

                // Create transaction record for audit
                txRepo.save(TransactionRecord.builder()
                        .reference("FDMAT-" + fd.getId())
                        .type(TransactionType.NEFT) // or FD_MATURITY type if you add it
                        .status(TransactionStatus.SUCCESS)
                        .toAccount(acc)
                        .amount(fd.getMaturityAmount())
                        .narration("FD matured and credited #" + fd.getId())
                        .createdAt(LocalDateTime.now())
                        .processedAt(LocalDateTime.now())
                        .build());

                notificationService.sendEmailAsync(fd.getOwner().getEmail(), "FD Matured",
                        "Your FD #" + fd.getId() + " matured and amount " + fd.getMaturityAmount() + " credited to account " + acc.getId());
            });
        } else {
            notificationService.sendEmailAsync(fd.getOwner().getEmail(), "FD Matured",
                    "Your FD #" + fd.getId() + " matured. Maturity amount: " + fd.getMaturityAmount() + ". Please collect from branch or link an account.");
        }

        // Auto renew if enabled
        if (Boolean.TRUE.equals(fd.getAutoRenew())) {
            // create a new FD with same tenure/principal = maturityAmount (simple renewal)
            CreateFDRequest create = new CreateFDRequest();
            create.setPrincipal(fd.getMaturityAmount());
            create.setAnnualInterestRate(fd.getAnnualInterestRate());
            create.setTenureMonths(fd.getTenureMonths());
            create.setLinkedAccountId(fd.getLinkedAccount() != null ? fd.getLinkedAccount().getId() : null);
            create.setAutoRenew(true);

            // We do not debit lifecycle here — it's auto-rolled
            FDResponse newFd = createFD(create, fd.getOwner().getUsername()); // careful: User entity may not have username loaded -> use a different approach
            // mark original as RENEWED
            fd.setStatus(DepositStatus.RENEWED);
            fdRepo.save(fd);

            notificationService.sendEmailAsync(fd.getOwner().getEmail(), "FD Auto-Renewed",
                    "Your FD #" + fd.getId() + " has been auto-renewed as FD #" + newFd.getId());
        }
    }

    private void handleRDMaturity(RecurringDeposit rd) {
        rd.setStatus(DepositStatus.MATURED);
        rd.setMaturedAt(LocalDateTime.now());
        rdRepo.save(rd);

        if (rd.getLinkedAccount() != null) {
            accountRepository.findByIdForUpdate(rd.getLinkedAccount().getId()).ifPresent(acc -> {
                acc.setBalance(acc.getBalance().add(rd.getMaturityAmount()));
                accountRepository.save(acc);

                txRepo.save(TransactionRecord.builder()
                        .reference("RDMAT-" + rd.getId())
                        .type(TransactionType.NEFT)
                        .status(TransactionStatus.SUCCESS)
                        .toAccount(acc)
                        .amount(rd.getMaturityAmount())
                        .narration("RD matured and credited #" + rd.getId())
                        .createdAt(LocalDateTime.now())
                        .processedAt(LocalDateTime.now())
                        .build());

                notificationService.sendEmailAsync(rd.getOwner().getEmail(), "RD Matured",
                        "Your RD #" + rd.getId() + " matured and amount " + rd.getMaturityAmount() + " credited to account " + acc.getId());
            });
        } else {
            notificationService.sendEmailAsync(rd.getOwner().getEmail(), "RD Matured",
                    "Your RD #" + rd.getId() + " matured. Maturity amount: " + rd.getMaturityAmount() + ". Please collect from branch or link an account.");
        }
    }

    // ---------- Interest calculator helpers ----------
    // For FD: simple annual compounding monthly frequency approximation
    private BigDecimal calculateFDMaturityAmount(BigDecimal principal, Double annualRatePercent, Integer months) {
        // We'll compound monthly: A = P * (1 + r/12)^(months)
        BigDecimal r = BigDecimal.valueOf(annualRatePercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal base = BigDecimal.ONE.add(r.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP));
        BigDecimal exponent = BigDecimal.valueOf(1);
        for (int i = 0; i < months; i++) exponent = exponent.multiply(base);
        BigDecimal amount = principal.multiply(exponent);
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    // For RD: monthly instalment compounded monthly
    private BigDecimal calculateRDMaturityAmount(BigDecimal monthly, Double annualRatePercent, Integer months) {
        // formula: M = monthly * [ ( (1+r)^n -1 ) / r ] * (1+r)
        BigDecimal r = BigDecimal.valueOf(annualRatePercent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP).divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        BigDecimal pow = BigDecimal.ONE;
        for (int i = 0; i < months; i++) pow = pow.multiply(onePlusR);
        BigDecimal numerator = pow.subtract(BigDecimal.ONE);
        if (r.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal total = monthly.multiply(BigDecimal.valueOf(months));
            return total.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal amount = monthly.multiply(numerator.divide(r, 10, RoundingMode.HALF_UP)).multiply(onePlusR);
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- Converters ----------
    private FDResponse toFDResponse(FixedDeposit fd) {
        return FDResponse.builder()
                .id(fd.getId())
                .principal(fd.getPrincipal())
                .annualInterestRate(fd.getAnnualInterestRate())
                .tenureMonths(fd.getTenureMonths())
                .startDate(fd.getStartDate())
                .maturityDate(fd.getMaturityDate())
                .maturityAmount(fd.getMaturityAmount())
                .status(fd.getStatus().name())
                .autoRenew(fd.getAutoRenew())
                .linkedAccountId(fd.getLinkedAccount() != null ? fd.getLinkedAccount().getId() : null)
                .build();
    }

    private RDResponse toRDResponse(RecurringDeposit rd) {
        return RDResponse.builder()
                .id(rd.getId())
                .monthlyInstallment(rd.getMonthlyInstallment())
                .annualInterestRate(rd.getAnnualInterestRate())
                .tenureMonths(rd.getTenureMonths())
                .startDate(rd.getStartDate())
                .maturityDate(rd.getMaturityDate())
                .maturityAmount(rd.getMaturityAmount())
                .status(rd.getStatus().name())
                .linkedAccountId(rd.getLinkedAccount() != null ? rd.getLinkedAccount().getId() : null)
                .build();
    }
}
