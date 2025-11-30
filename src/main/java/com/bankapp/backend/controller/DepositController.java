package com.bankapp.backend.controller;

import com.bankapp.backend.dto.*;
import com.bankapp.backend.service.DepositService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final DepositService depositService;

    @PostMapping("/fd")
    public ResponseEntity<FDResponse> createFD(@Valid @RequestBody CreateFDRequest req, Authentication auth) {
        var resp = depositService.createFD(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/fd/my")
    public List<FDResponse> listMyFDs(Authentication auth) {
        return depositService.listMyFDs(auth.getName());
    }

    @GetMapping("/fd/{id}")
    public FDResponse getFD(@PathVariable Long id, Authentication auth) {
        return depositService.getFD(id, auth.getName());
    }

    @PostMapping("/fd/{id}/cancel")
    public ResponseEntity<Void> cancelFD(@PathVariable Long id, Authentication auth) {
        depositService.cancelFD(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rd")
    public ResponseEntity<RDResponse> createRD(@Valid @RequestBody CreateRDRequest req, Authentication auth) {
        var resp = depositService.createRD(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/rd/my")
    public List<RDResponse> listMyRDs(Authentication auth) {
        return depositService.listMyRDs(auth.getName());
    }

    @GetMapping("/rd/{id}")
    public RDResponse getRD(@PathVariable Long id, Authentication auth) {
        return depositService.getRD(id, auth.getName());
    }

    @PostMapping("/rd/{id}/cancel")
    public ResponseEntity<Void> cancelRD(@PathVariable Long id, Authentication auth) {
        depositService.cancelRD(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
