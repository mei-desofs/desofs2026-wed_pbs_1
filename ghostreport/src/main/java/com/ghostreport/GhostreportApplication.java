package com.ghostreport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GhostreportApplication {

	public static void main(String[] args) {
		SpringApplication.run(GhostreportApplication.class, args);
	}

}
