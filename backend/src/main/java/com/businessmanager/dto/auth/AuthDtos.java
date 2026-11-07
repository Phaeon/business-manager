package com.businessmanager.dto.auth;

import com.businessmanager.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            UserInfo user
    ) {
    }

    public record UserInfo(
            String id,
            String firstName,
            String lastName,
            String email,
            Role role
    ) {
    }
}
