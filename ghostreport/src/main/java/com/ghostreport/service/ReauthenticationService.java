package com.ghostreport.service;

import com.ghostreport.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReauthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public ReauthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public void requireCurrentPassword(Authentication authentication, String password, String action) {
        if (authentication == null || !authentication.isAuthenticated() || password == null || password.isBlank()) {
            reject(authentication, action);
        }

        String username = username(authentication);
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> reject(authentication, action));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            reject(authentication, action);
        }

        auditLogService.log("REAUTHENTICATION_SUCCESS", "USER", user.getId(), action + " confirmed");
    }

    private String username(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName();
    }

    private ResponseStatusException reject(Authentication authentication, String action) {
        auditLogService.log(
                "REAUTHENTICATION_FAILED",
                "USER",
                null,
                action + " denied for " + (authentication == null ? "anonymous" : authentication.getName())
        );
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reauthentication required");
    }
}
