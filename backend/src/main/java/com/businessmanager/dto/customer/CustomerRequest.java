package com.businessmanager.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank(message = "Le nom est obligatoire") String name,
        String company,
        @NotBlank @Email(message = "Email invalide") String email,
        String phone,
        String address,
        String notes
) {
}
