package com.smanioto.bank.accounts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
    @NotNull UUID fromAccountId,
    @NotNull UUID toAccountId,
    @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount
) {
}
