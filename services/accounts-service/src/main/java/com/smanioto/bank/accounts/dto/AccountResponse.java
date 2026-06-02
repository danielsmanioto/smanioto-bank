package com.smanioto.bank.accounts.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, UUID customerId, String bank, String branch, String number, BigDecimal balance) {
}
