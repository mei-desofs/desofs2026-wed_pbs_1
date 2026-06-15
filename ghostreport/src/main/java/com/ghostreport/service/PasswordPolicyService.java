package com.ghostreport.service;

import com.ghostreport.model.PasswordHistory;
import com.ghostreport.model.User;
import com.ghostreport.repository.PasswordHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class PasswordPolicyService {

    private static final Set<String> COMPROMISED_PASSWORDS = Set.of(
            "p@ssw0rd1234!",
            "qwerty12345!",
            "letmein12345!",
            "welcome12345!",
            "changeme12345!",
            "ghostreport12345!"
    );
    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;
    private static final Set<String> CONTEXT_WORDS = Set.of(
            "ghostreport",
            "admin",
            "analyst",
            "auditor",
            "reporter"
    );

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordPolicyService(
            PasswordHistoryRepository passwordHistoryRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void validateNewPassword(User user, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        if (newPassword.length() < MIN_LENGTH || newPassword.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password length is invalid");
        }

        String normalized = newPassword.toLowerCase(Locale.ROOT);
        if (COMPROMISED_PASSWORDS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is compromised");
        }
        if (containsContextSpecificWord(user, normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password contains context-specific words");
        }

        if (user != null && passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password was already used");
        }

        if (user != null) {
            boolean reused = passwordHistoryRepository.findTop5ByUserOrderByCreatedAtDesc(user)
                    .stream()
                    .map(PasswordHistory::getPasswordHash)
                    .anyMatch(hash -> passwordEncoder.matches(newPassword, hash));
            if (reused) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password was already used");
            }
        }
    }

    public void rememberPassword(User user, String passwordHash) {
        PasswordHistory history = new PasswordHistory();
        history.setUser(user);
        history.setPasswordHash(passwordHash);
        passwordHistoryRepository.save(history);
    }

    private boolean containsContextSpecificWord(User user, String normalizedPassword) {
        if (CONTEXT_WORDS.stream().anyMatch(normalizedPassword::contains)) {
            return true;
        }
        if (user == null) {
            return false;
        }
        return containsUserValue(normalizedPassword, user.getUsername())
                || containsUserValue(normalizedPassword, emailLocalPart(user.getEmail()));
    }

    private boolean containsUserValue(String normalizedPassword, String value) {
        return value != null
                && value.trim().length() >= 4
                && normalizedPassword.contains(value.trim().toLowerCase(Locale.ROOT));
    }

    private String emailLocalPart(String email) {
        if (email == null) {
            return null;
        }
        int separator = email.indexOf('@');
        return separator > 0 ? email.substring(0, separator) : email;
    }
}
