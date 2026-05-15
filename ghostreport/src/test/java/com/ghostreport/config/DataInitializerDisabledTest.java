package com.ghostreport.config;

import com.ghostreport.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:data-initializer-disabled-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ghostreport.seed-users.enabled=false",
        "ghostreport.backup-dir=target/test-backups/data-initializer-disabled",
        "app.upload-dir=target/test-uploads/data-initializer-disabled"
})
@ActiveProfiles("test")
class DataInitializerDisabledTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void defaultUsersAreNotCreatedWhenSeedingIsDisabled() {
        assertThat(userRepository.findByUsername("admin")).isEmpty();
        assertThat(userRepository.findByUsername("analyst")).isEmpty();
        assertThat(userRepository.findByUsername("auditor")).isEmpty();
    }
}
