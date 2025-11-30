package com.bankapp.backend.controller;

import com.bankapp.backend.dto.TransferRequest;
import com.bankapp.backend.dto.TransferResponse;
import com.bankapp.backend.entity.TransactionRecord;
import com.bankapp.backend.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/imps")
    public ResponseEntity<TransferResponse> impsTransfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication
    ) {
        var resp = transferService.doImps(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/neft")
    public ResponseEntity<TransferResponse> neftTransfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication
    ) {
        var resp = transferService.createNeft(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }

    @GetMapping("/{reference}")
    public ResponseEntity<TransactionRecord> getByReference(@PathVariable String reference) {
        var tx = transferService.findByReference(reference);
        return ResponseEntity.ok(tx);
    }
}
