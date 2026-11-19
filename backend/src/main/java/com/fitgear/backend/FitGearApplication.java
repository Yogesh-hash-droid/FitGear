package com.fitgear.backend;

import com.fitgear.backend.config.AivenTrustStoreInstaller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FitGearApplication {

	public static void main(String[] args) {
		// Aiven CA (Render secret file) -> JVM truststore, before any DB connection.
		// No-op in local development where the PEM file is absent.
		AivenTrustStoreInstaller.installIfConfigured();
		SpringApplication.run(FitGearApplication.class, args);
	}

}
