package com.bankapp.backend.controller;

import com.bankapp.backend.dto.AccountResponse;
import com.bankapp.backend.dto.OpenAccountRequest;
import com.bankapp.backend.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // POST /api/accounts/open
    @PostMapping("/open")
    public ResponseEntity<AccountResponse> openAccount(
            @Valid @RequestBody OpenAccountRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        var response = accountService.openAccount(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/accounts/my
    @GetMapping("/my")
    public List<AccountResponse> getMyAccounts(Authentication authentication) {
        String username = authentication.getName();
        return accountService.getMyAccounts(username);
    }

    // GET /api/accounts/{id}
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication.getName();
        var response = accountService.getAccountDetails(id, username);
        return ResponseEntity.ok(response);
    }
}
