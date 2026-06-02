package com.smanioto.bank.accounts.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(UUID transferId, UUID fromAccountId, UUID toAccountId, BigDecimal amount) {
}
