package com.smanioto.bank.people.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
    @NotBlank @Size(min = 3, max = 120) String fullName,
    @NotBlank @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos numéricos (validação de formato)") String cpf,
    @NotBlank @Email String email
) {
}
