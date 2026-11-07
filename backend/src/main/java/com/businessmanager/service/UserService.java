package com.businessmanager.service;

import com.businessmanager.dto.user.UserDtos.AssigneeDto;
import com.businessmanager.dto.user.UserDtos.ChangePasswordRequest;
import com.businessmanager.dto.user.UserDtos.UserCreateRequest;
import com.businessmanager.dto.user.UserDtos.UserResponse;
import com.businessmanager.entity.User;
import com.businessmanager.exception.BadRequestException;
import com.businessmanager.exception.ConflictException;
import com.businessmanager.exception.ResourceNotFoundException;
import com.businessmanager.mapper.UserMapper;
import com.businessmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("lastName", "firstName"))
                .stream().map(userMapper::toResponse).toList();
    }

    /** Collaborateurs actifs, utilisables comme responsables de tâche. */
    public List<AssigneeDto> assignable() {
        return userRepository.findAll(Sort.by("lastName", "firstName")).stream()
                .filter(User::isEnabled)
                .map(userMapper::toAssignee)
                .toList();
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Un compte existe déjà avec cet email");
        }
        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse setEnabled(UUID id, boolean enabled, User current) {
        if (current.getId().equals(id) && !enabled) {
            throw new BadRequestException("Vous ne pouvez pas désactiver votre propre compte");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
        user.setEnabled(enabled);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(User current, ChangePasswordRequest request) {
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Le mot de passe actuel est incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
