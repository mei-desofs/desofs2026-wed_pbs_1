package com.ghostreport.service;

import com.ghostreport.model.PasswordHistory;
import com.ghostreport.model.User;
import com.ghostreport.repository.PasswordHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordPolicyServiceTest {

    private static final String CURRENT_PASSWORD = "CurrentPassword123!";
    private static final String NEW_PASSWORD = "FreshPassword123!";

    private final PasswordHistoryRepository passwordHistoryRepository =
            mock(PasswordHistoryRepository.class);
    private final PasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();
    private final PasswordPolicyService service =
            new PasswordPolicyService(passwordHistoryRepository, passwordEncoder);

    @Test
    void rejectsNullPassword() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validateNewPassword(null, null)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Password is required", exception.getReason());
    }

    @Test
    void rejectsBlankPassword() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validateNewPassword(null, "   ")
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Password is required", exception.getReason());
    }

    @Test
    void rejectsCompromisedPasswordCaseInsensitively() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validateNewPassword(null, "P@SSW0RD1234!")
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Password is compromised", exception.getReason());
    }

    @Test
    void rejectsCurrentPasswordReuse() {
        User user = userWithPassword(CURRENT_PASSWORD);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validateNewPassword(user, CURRENT_PASSWORD)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Password was already used", exception.getReason());
        verify(passwordHistoryRepository, never())
                .findTop5ByUserOrderByCreatedAtDesc(any());
    }

    @Test
    void rejectsRecentPasswordHistoryReuse() {
        User user = userWithPassword(CURRENT_PASSWORD);
        PasswordHistory history = new PasswordHistory();
        history.setUser(user);
        history.setPasswordHash(passwordEncoder.encode(NEW_PASSWORD));
        when(passwordHistoryRepository.findTop5ByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(history));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validateNewPassword(user, NEW_PASSWORD)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Password was already used", exception.getReason());
    }

    @Test
    void acceptsFreshPasswordForUser() {
        User user = userWithPassword(CURRENT_PASSWORD);
        when(passwordHistoryRepository.findTop5ByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of());

        assertDoesNotThrow(
                () -> service.validateNewPassword(user, NEW_PASSWORD)
        );
    }

    @Test
    void acceptsFreshPasswordWhenHistoryDoesNotMatch() {
        User user = userWithPassword(CURRENT_PASSWORD);
        PasswordHistory history = new PasswordHistory();
        history.setUser(user);
        history.setPasswordHash(passwordEncoder.encode("DifferentPassword123!"));
        when(passwordHistoryRepository.findTop5ByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(history));

        assertDoesNotThrow(
                () -> service.validateNewPassword(user, NEW_PASSWORD)
        );
    }

    @Test
    void acceptsFreshPasswordWithoutUser() {
        assertDoesNotThrow(
                () -> service.validateNewPassword(null, NEW_PASSWORD)
        );

        verify(passwordHistoryRepository, never())
                .findTop5ByUserOrderByCreatedAtDesc(any());
    }

    @Test
    void rememberPasswordStoresHistoryEntry() {
        User user = userWithPassword(CURRENT_PASSWORD);
        when(passwordHistoryRepository.save(any(PasswordHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.rememberPassword(user, "encoded-new-password");

        ArgumentCaptor<PasswordHistory> captor =
                ArgumentCaptor.forClass(PasswordHistory.class);
        verify(passwordHistoryRepository).save(captor.capture());

        assertSame(user, captor.getValue().getUser());
        assertEquals("encoded-new-password", captor.getValue().getPasswordHash());
    }

    private User userWithPassword(String rawPassword) {
        User user = new User();
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }
}
