package com.bankapp.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddChargeRequest {
    private Long cardId;
    private BigDecimal amount;
    private String narration; // optional: "Amazon Purchase"
}
