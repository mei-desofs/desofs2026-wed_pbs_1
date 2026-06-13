package com.ghostreport;

import com.ghostreport.config.MfaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(MfaProperties.class)
public class GhostreportApplication {

	public static void main(String[] args) {
		SpringApplication.run(GhostreportApplication.class, args);
	}

}
