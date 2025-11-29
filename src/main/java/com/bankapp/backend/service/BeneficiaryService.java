package com.bankapp.backend.service;

import com.bankapp.backend.dto.AddBeneficiaryRequest;
import com.bankapp.backend.dto.BeneficiaryResponse;
import com.bankapp.backend.entity.Account;
import com.bankapp.backend.entity.Beneficiary;
import com.bankapp.backend.entity.User;
import com.bankapp.backend.repository.AccountRepository;
import com.bankapp.backend.repository.BeneficiaryRepository;
import com.bankapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public BeneficiaryResponse addBeneficiary(AddBeneficiaryRequest req, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Owner user not found"));

        // prevent duplicates for same owner + accountNumber
        if (beneficiaryRepository.existsByOwnerAndAccountNumber(owner, req.getAccountNumber())) {
            throw new RuntimeException("Beneficiary with this account number already exists for this user");
        }

        // check internal account
        var maybeAccount = accountRepository.findByAccountNumber(req.getAccountNumber());

        Beneficiary b = Beneficiary.builder()
                .owner(owner)
                .beneficiaryName(req.getBeneficiaryName())
                .accountNumber(req.getAccountNumber())
                .bankName(req.getBankName())
                .ifsc(req.getIfsc())
                .internal(maybeAccount.isPresent())
                .beneficiaryUserId(maybeAccount.map(Account::getOwner).map(User::getId).orElse(null))
                .build();

        b = beneficiaryRepository.save(b);
        return toResponse(b);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> listMyBeneficiaries(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Owner user not found"));
        var list = beneficiaryRepository.findByOwner(owner);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiary(Long id, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Owner user not found"));
        var b = beneficiaryRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found or not owned by you"));
        return toResponse(b);
    }

    @Transactional
    public void deleteBeneficiary(Long id, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Owner user not found"));
        var b = beneficiaryRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found or not owned by you"));
        beneficiaryRepository.delete(b);
    }

    // simple verification: returns true + internal info if account exists in our system
    @Transactional(readOnly = true)
    public BeneficiaryResponse verifyAccountNumber(String accountNumber) {
        var maybeAccount = accountRepository.findByAccountNumber(accountNumber);
        if (maybeAccount.isEmpty()) {
            return BeneficiaryResponse.builder()
                    .accountNumber(accountNumber)
                    .internal(false)
                    .build();
        }
        var account = maybeAccount.get();
        return BeneficiaryResponse.builder()
                .accountNumber(account.getAccountNumber())
                .internal(true)
                .beneficiaryUserId(account.getOwner().getId())
                .beneficiaryName(account.getOwner().getFullName())
                .ownerId(account.getOwner().getId()) // owner here is beneficiary's owner
                .ownerUsername(account.getOwner().getUsername())
                .build();
    }

    private BeneficiaryResponse toResponse(Beneficiary b) {
        return BeneficiaryResponse.builder()
                .id(b.getId())
                .beneficiaryName(b.getBeneficiaryName())
                .accountNumber(b.getAccountNumber())
                .bankName(b.getBankName())
                .ifsc(b.getIfsc())
                .internal(b.isInternal())
                .beneficiaryUserId(b.getBeneficiaryUserId())
                .ownerId(b.getOwner().getId())
                .ownerUsername(b.getOwner().getUsername())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
