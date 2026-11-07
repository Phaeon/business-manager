package com.businessmanager.dto.user;

import com.businessmanager.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UserDtos {

    public record UserCreateRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password,
            @NotNull Role role
    ) {
    }

    public record UserResponse(UUID id, String firstName, String lastName, String email, Role role, boolean enabled) {
    }

    public record AssigneeDto(UUID id, String name) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String newPassword
    ) {
    }
}
