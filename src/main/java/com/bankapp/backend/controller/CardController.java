package com.bankapp.backend.controller;

import com.bankapp.backend.dto.*;
import com.bankapp.backend.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping("/apply")
    public ResponseEntity<CardResponse> apply(@Valid @RequestBody ApplyCardRequest req, Authentication auth) {
        var resp = cardService.applyCard(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // Admin-only: issue the card (you can secure this endpoint to ROLE_ADMIN)
    /*@PostMapping("/issue/{id}")
    public ResponseEntity<CardResponse> issue(@PathVariable Long id) {
        var resp = cardService.issueCard(id);
        return ResponseEntity.ok(resp);
    }*/
    @PostMapping("/activate/{id}")
    public ResponseEntity<CardResponse> activate(@PathVariable Long id, Authentication auth) {
        var resp = cardService.activateCard(id, auth.getName());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/block/{id}")
    public ResponseEntity<CardResponse> block(@PathVariable Long id, @RequestBody(required = false) BlockUnblockRequest req, Authentication auth) {
        var resp = cardService.blockCard(id, auth.getName(), req == null ? null : req.getReason());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/unblock/{id}")
    public ResponseEntity<CardResponse> unblock(@PathVariable Long id, Authentication auth) {
        var resp = cardService.unblockCard(id, auth.getName());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/international")
    public ResponseEntity<CardResponse> setInternational(@PathVariable Long id, @RequestParam boolean enabled, Authentication auth) {
        var resp = cardService.setInternationalUsage(id, auth.getName(), enabled);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/pay-bill")
    public ResponseEntity<CardResponse> payBill(@Valid @RequestBody PayCardBillRequest req, Authentication auth) {
        var resp = cardService.payCardBill(req, auth.getName());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/add-charge")
    public ResponseEntity<CardResponse> addCharge(@RequestBody AddChargeRequest request, Authentication authentication) {
        String username = authentication.getName();
        var resp = cardService.addCharge(request, username);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/my")
    public ResponseEntity<List<CardResponse>> listMy(Authentication auth) {
        var list = cardService.listMyCards(auth.getName());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getOne(@PathVariable Long id, Authentication auth) {
        // reuse listMy & service to check ownership - or add getOne method
        var user = auth.getName();
        var resp = cardService.listMyCards(user).stream().filter(c -> c.getId().equals(id)).findFirst()
                .orElseThrow(() -> new RuntimeException("Not found or not owner"));
        return ResponseEntity.ok(resp);
    }
}
