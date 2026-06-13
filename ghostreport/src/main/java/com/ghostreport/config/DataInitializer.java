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
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@ghostreport.local");
                admin.setPasswordHash(passwordEncoder.encode("AdminPassword123!"));
                admin.setRole(UserRole.ADMIN);
                admin.setActive(true);
                userRepository.save(admin);
            }

            if (userRepository.findByUsername("analyst").isEmpty()) {
                User analyst = new User();
                analyst.setUsername("analyst");
                analyst.setEmail("analyst@ghostreport.local");
                analyst.setPasswordHash(passwordEncoder.encode("AnalystPassword123!"));
                analyst.setRole(UserRole.ANALYST);
                analyst.setActive(true);
                userRepository.save(analyst);
            }

            if (userRepository.findByUsername("user").isEmpty()) {
                User user = new User();
                user.setUsername("user");
                user.setEmail("user@ghostreport.local");
                user.setPasswordHash(passwordEncoder.encode("UserPassword123!"));
                user.setRole(UserRole.USER);
                user.setActive(true);
                userRepository.save(user);
            }
        };
    }

    @Bean
    @Profile({"dev", "test"})
    @ConditionalOnProperty(prefix = "ghostreport.seed-users", name = "enabled", havingValue = "true")
    CommandLineRunner initAuditor(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("auditor").isEmpty()) {
                User auditor = new User();
                auditor.setUsername("auditor");
                auditor.setEmail("auditor@ghostreport.local");
                auditor.setPasswordHash(passwordEncoder.encode("AuditorPassword123!"));
                auditor.setRole(UserRole.AUDITOR);
                auditor.setActive(true);
                userRepository.save(auditor);
            }
        };
    }
}
