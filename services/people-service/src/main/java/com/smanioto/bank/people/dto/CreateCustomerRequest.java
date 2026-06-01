package com.smanioto.bank.people.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
    @NotBlank @Size(min = 3, max = 120) String fullName,
    @NotBlank @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos numéricos") String cpf,
    @NotBlank @Email String email
) {

    @AssertTrue(message = "CPF inválido")
    public boolean isCpfValid() {
        if (cpf == null || cpf.length() != 11) {
            return false;
        }
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        return validateDigit(cpf, 9, 10) && validateDigit(cpf, 10, 11);
    }

    private boolean validateDigit(String value, int length, int weightStart) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.getNumericValue(value.charAt(i)) * (weightStart - i);
        }
        int remainder = sum % 11;
        int expected = remainder < 2 ? 0 : 11 - remainder;
        return expected == Character.getNumericValue(value.charAt(length));
    }
}
