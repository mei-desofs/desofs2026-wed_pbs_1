package com.ghostreport.service;

import com.ghostreport.dto.CreateUserRequest;
import com.ghostreport.dto.UpdateUserRequest;
import com.ghostreport.dto.UserResponse;
import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.ghostreport.validation.ValidationConstants.trim;
import static com.ghostreport.validation.ValidationConstants.upper;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final PasswordPolicyService passwordPolicyService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            PasswordPolicyService passwordPolicyService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.passwordPolicyService = passwordPolicyService;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.isActive()
                ))
                .toList();
    }

    public UserResponse createUser(CreateUserRequest request) {
        String username = trim(request.getUsername());
        String email = trim(request.getEmail());

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(upper(request.getRole()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        passwordPolicyService.validateNewPassword(null, request.getPassword());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setActive(true);

        User saved = userRepository.save(user);
        passwordPolicyService.rememberPassword(saved, saved.getPasswordHash());

        logger.info(
                "User created with id={}, role={}",
                saved.getId(),
                saved.getRole()
        );

        auditLogService.log(
                "USER_CREATED",
                "USER",
                saved.getId(),
                "User created with role " + saved.getRole()
        );

        return toResponse(saved);
    }

    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String username = trim(request.getUsername());
        String email = trim(request.getEmail());
        UserRole role = parseRole(request.getRole());
        boolean active = Boolean.TRUE.equals(request.getActive());

        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
                });

        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
                });

        boolean removesActiveAdmin = user.isActive()
                && user.getRole() == UserRole.ADMIN
                && (!active || role != UserRole.ADMIN);
        if (removesActiveAdmin && userRepository.countByRoleAndActiveTrue(UserRole.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one active administrator is required");
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(active);

        User saved = userRepository.save(user);
        auditLogService.log(
                "USER_UPDATED",
                "USER",
                saved.getId(),
                "User updated with role " + saved.getRole() + " and active=" + saved.isActive()
        );
        logger.info("USER_UPDATED for user id={}, role={}, active={}", saved.getId(), saved.getRole(), saved.isActive());

        return toResponse(saved);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            auditLogService.log("PASSWORD_CHANGE_REJECTED", "USER", user.getId(), "Current password validation failed");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is invalid");
        }

        passwordPolicyService.validateNewPassword(user, newPassword);
        String newHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHash);
        User saved = userRepository.save(user);
        passwordPolicyService.rememberPassword(saved, newHash);
        auditLogService.log("PASSWORD_CHANGED", "USER", saved.getId(), "Password changed by authenticated user");
    }

    public UserResponse setActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isActive() == active) {
            return toResponse(user);
        }

        if (!active
                && user.getRole() == UserRole.ADMIN
                && userRepository.countByRoleAndActiveTrue(UserRole.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one active administrator is required");
        }

        user.setActive(active);
        User saved = userRepository.save(user);

        String action = active ? "USER_ACTIVATED" : "USER_DEACTIVATED";
        auditLogService.log(action, "USER", saved.getId(), "User active status changed");
        logger.info("{} for user id={}", action, saved.getId());

        return toResponse(saved);
    }

    private UserRole parseRole(String roleValue) {
        try {
            return UserRole.valueOf(upper(roleValue));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request");
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive()
        );
    }
}
