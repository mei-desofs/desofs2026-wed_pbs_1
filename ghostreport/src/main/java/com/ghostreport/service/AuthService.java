package com.ghostreport.service;

import com.ghostreport.dto.AuthResponse;
import com.ghostreport.dto.LoginRequest;
import com.ghostreport.model.User;
import com.ghostreport.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AuditLogService auditLogService,
            UserRepository userRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        User storedUser = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (storedUser != null && !storedUser.isActive()) {
            auditLogService.log(
                    "LOGIN_BLOCKED_INACTIVE_USER",
                    "USER",
                    storedUser.getId(),
                    "Login blocked for inactive user"
            );
            throw new BadCredentialsException("Invalid credentials");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = (UserDetails) authentication.getPrincipal();
        auditLogService.log(
                "LOGIN_SUCCESS",
                "USER",
                storedUser != null ? storedUser.getId() : null,
                "User logged in successfully"
        );

        String token = jwtService.generateToken(user);
        String role = user.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse("");

        return new AuthResponse(
                token,
                "Bearer",
                user.getUsername(),
                role,
                jwtService.getExpirationSeconds()
        );
    }
}
