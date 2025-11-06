package com.brokerx.matching_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MatchingServiceApplication {

	/**
	 * Main entry point for the matching service Spring Boot application.
	 * Enables scheduling for Outbox Pattern and Saga Compensation
	 */
	public static void main(String[] args) {
		SpringApplication.run(MatchingServiceApplication.class, args);
	}

}
