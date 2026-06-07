package com.ghostreport.repository;

import com.ghostreport.model.PasswordResetToken;
import com.ghostreport.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUserAndUsedAtIsNull(User user);
}
