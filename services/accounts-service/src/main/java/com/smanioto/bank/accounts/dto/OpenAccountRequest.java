package com.smanioto.bank.accounts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record OpenAccountRequest(
    @NotNull UUID customerId,
    @NotBlank String bank,
    @NotBlank String branch,
    @NotBlank String number,
    @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal initialBalance
) {
}
