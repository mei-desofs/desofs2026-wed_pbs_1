package com.ghostreport.config;

import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerTest {

    @Test
    void seedUsersRefreshExistingDemoAccountsForDevAndTest() throws Exception {
        User admin = staleUser("admin", UserRole.ANALYST);
        User analyst = staleUser("analyst", UserRole.ADMIN);
        User auditor = staleUser("auditor", UserRole.ADMIN);
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("analyst")).thenReturn(Optional.of(analyst));
        when(userRepository.findByUsername("auditor")).thenReturn(Optional.of(auditor));
        when(passwordEncoder.encode(any())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));

        DataInitializer initializer = new DataInitializer();
        CommandLineRunner usersRunner = initializer.initUsers(userRepository, passwordEncoder);
        CommandLineRunner auditorRunner = initializer.initAuditor(userRepository, passwordEncoder);

        usersRunner.run();
        auditorRunner.run();

        assertSeedUser(admin, "admin@ghostreport.local", "hash:AdminPassword123!", UserRole.ADMIN);
        assertSeedUser(analyst, "analyst@ghostreport.local", "hash:AnalystPassword123!", UserRole.ANALYST);
        assertSeedUser(auditor, "auditor@ghostreport.local", "hash:AuditorPassword123!", UserRole.AUDITOR);
        verify(userRepository).save(admin);
        verify(userRepository).save(analyst);
        verify(userRepository).save(auditor);
    }

    private User staleUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@old.local");
        user.setPasswordHash("old-hash");
        user.setRole(role);
        user.setActive(false);
        return user;
    }

    private void assertSeedUser(User user, String email, String passwordHash, UserRole role) {
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(user.getRole()).isEqualTo(role);
        assertThat(user.isActive()).isTrue();
    }
}
