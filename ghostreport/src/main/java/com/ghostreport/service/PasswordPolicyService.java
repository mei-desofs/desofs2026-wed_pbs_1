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

        String normalized = newPassword.toLowerCase(Locale.ROOT);
        if (COMPROMISED_PASSWORDS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is compromised");
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
}
