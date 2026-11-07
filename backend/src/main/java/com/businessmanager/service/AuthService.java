package com.businessmanager.service;

import com.businessmanager.dto.auth.AuthDtos.AuthResponse;
import com.businessmanager.dto.auth.AuthDtos.LoginRequest;
import com.businessmanager.dto.auth.AuthDtos.RefreshRequest;
import com.businessmanager.dto.auth.AuthDtos.UserInfo;
import com.businessmanager.entity.User;
import com.businessmanager.repository.UserRepository;
import com.businessmanager.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        try {
            String token = request.refreshToken();
            if (!jwtService.isRefreshToken(token)) {
                throw new BadCredentialsException("Refresh token invalide");
            }
            User user = userRepository.findByEmail(jwtService.extractUsername(token))
                    .orElseThrow(() -> new BadCredentialsException("Refresh token invalide"));
            if (!user.isEnabled() || !jwtService.isTokenValid(token, user)) {
                throw new BadCredentialsException("Refresh token invalide");
            }
            return buildAuthResponse(user);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Refresh token invalide ou expiré");
        }
    }

    public UserInfo me(User user) {
        return toInfo(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                toInfo(user));
    }

    private UserInfo toInfo(User user) {
        return new UserInfo(user.getId().toString(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole());
    }
}
