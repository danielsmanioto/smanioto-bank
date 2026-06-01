package com.smanioto.bank.accounts.dto;

import com.smanioto.bank.accounts.model.MovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
    UUID id,
    UUID transferId,
    MovementType type,
    BigDecimal amount,
    String description,
    Instant createdAt
) {
}
