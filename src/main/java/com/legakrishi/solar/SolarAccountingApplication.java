package com.legakrishi.solar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.legakrishi.solar.repository")
@EntityScan(basePackages = "com.legakrishi.solar.model")
public class SolarAccountingApplication {
	public static void main(String[] args) {
		SpringApplication.run(SolarAccountingApplication.class, args);
	}
}
