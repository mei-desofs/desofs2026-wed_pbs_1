package com.ghostreport.service;

import com.ghostreport.dto.AuthResponse;
import com.ghostreport.dto.LoginRequest;
import com.ghostreport.dto.MfaVerifyRequest;
import com.ghostreport.model.User;
import com.ghostreport.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final MfaChallengeService mfaChallengeService;
    private final UserDetailsService userDetailsService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AuditLogService auditLogService,
            UserRepository userRepository,
            MfaChallengeService mfaChallengeService,
            UserDetailsService userDetailsService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.mfaChallengeService = mfaChallengeService;
        this.userDetailsService = userDetailsService;
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

        if (storedUser != null
                && mfaChallengeService.isMfaRequiredFor(storedUser.getRole())) {
            MfaChallengeService.MfaChallenge challenge = mfaChallengeService.createChallenge(storedUser);
            return AuthResponse.mfaRequired(
                    user.getUsername(),
                    storedUser.getRole().name(),
                    challenge.challengeId()
            );
        }

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

    public AuthResponse verifyMfa(MfaVerifyRequest request) {
        String username;
        try {
            username = mfaChallengeService.verifyChallenge(request.getChallengeId(), request.getCode());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid or expired verification code");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive() || !mfaChallengeService.isMfaRequiredFor(user.getRole())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String token = jwtService.generateToken(userDetails);
        auditLogService.log("LOGIN_SUCCESS", "USER", user.getId(), "User logged in after MFA");

        return new AuthResponse(
                token,
                "Bearer",
                userDetails.getUsername(),
                user.getRole().name(),
                jwtService.getExpirationSeconds()
        );
    }
}
