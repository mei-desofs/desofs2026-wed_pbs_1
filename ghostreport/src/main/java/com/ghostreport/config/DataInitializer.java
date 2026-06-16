package com.ghostreport.config;

import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    @Profile({"dev", "test"})
    @ConditionalOnProperty(prefix = "ghostreport.seed-users", name = "enabled", havingValue = "true")
    CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            upsertSeedUser(userRepository, passwordEncoder, "admin",
                    "admin@ghostreport.local", "AdminPassword123!", UserRole.ADMIN);
            upsertSeedUser(userRepository, passwordEncoder, "analyst",
                    "analyst@ghostreport.local", "AnalystPassword123!", UserRole.ANALYST);
        };
    }

    @Bean
    @Profile({"dev", "test"})
    @ConditionalOnProperty(prefix = "ghostreport.seed-users", name = "enabled", havingValue = "true")
    CommandLineRunner initAuditor(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            upsertSeedUser(userRepository, passwordEncoder, "auditor",
                    "auditor@ghostreport.local", "AuditorPassword123!", UserRole.AUDITOR);
        };
    }

    private void upsertSeedUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String email,
            String password,
            UserRole role
    ) {
        User user = userRepository.findByUsername(username).orElseGet(User::new);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
    }
}
