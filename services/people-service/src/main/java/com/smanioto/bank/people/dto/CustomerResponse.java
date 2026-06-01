package com.smanioto.bank.people.dto;

import java.util.UUID;

public record CustomerResponse(UUID id, String fullName, String cpf, String email) {
}
