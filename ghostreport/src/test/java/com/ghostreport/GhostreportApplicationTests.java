package com.ghostreport;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:ghostreport-context-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.show-sql=false",
		"ghostreport.backup-dir=target/test-backups/context",
		"app.upload-dir=target/test-uploads/context",
		"ghostreport.backup-enabled=true"
})
class GhostreportApplicationTests {

	@Test
	void contextLoads() {
	}

}
