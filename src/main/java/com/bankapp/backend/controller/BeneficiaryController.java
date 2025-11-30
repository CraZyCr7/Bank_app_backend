package com.bankapp.backend.controller;

import com.bankapp.backend.dto.AddBeneficiaryRequest;
import com.bankapp.backend.dto.BeneficiaryResponse;
import com.bankapp.backend.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> addBeneficiary(
            @Valid @RequestBody AddBeneficiaryRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        var resp = beneficiaryService.addBeneficiary(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/my")
    public List<BeneficiaryResponse> listMy(Authentication authentication) {
        String username = authentication.getName();
        return beneficiaryService.listMyBeneficiaries(username);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BeneficiaryResponse> getOne(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication.getName();
        var resp = beneficiaryService.getBeneficiary(id, username);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication.getName();
        beneficiaryService.deleteBeneficiary(id, username);
        return ResponseEntity.noContent().build();
    }

    // verify if an accountNumber belongs to an internal user
    @GetMapping("/verify")
    public ResponseEntity<BeneficiaryResponse> verify(@RequestParam String accountNumber) {
        var resp = beneficiaryService.verifyAccountNumber(accountNumber);
        return ResponseEntity.ok(resp);
    }
}
