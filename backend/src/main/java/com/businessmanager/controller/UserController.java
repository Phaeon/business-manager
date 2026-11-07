package com.businessmanager.controller;

import com.businessmanager.dto.user.UserDtos.AssigneeDto;
import com.businessmanager.dto.user.UserDtos.UserCreateRequest;
import com.businessmanager.dto.user.UserDtos.UserResponse;
import com.businessmanager.entity.User;
import com.businessmanager.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    /** Accessible à tous les rôles : sert à choisir un responsable de tâche. */
    @GetMapping("/assignable")
    public List<AssigneeDto> assignable() {
        return userService.assignable();
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PatchMapping("/{id}/enabled")
    public UserResponse setEnabled(@PathVariable UUID id,
                                   @RequestParam boolean value,
                                   @AuthenticationPrincipal User current) {
        return userService.setEnabled(id, value, current);
    }
}
