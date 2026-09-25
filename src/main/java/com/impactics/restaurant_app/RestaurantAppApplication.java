package com.impactics.restaurant_app;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@SpringBootApplication
public class RestaurantAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestaurantAppApplication.class, args);
	}

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Impactis Restaurant API")
						.version("1.0")
						.description("RESTful backend for the restaurant ordering platform."));
	}

	@Bean
	public Flyway forceFlywayMigration(DataSource dataSource) {
		Flyway flyway = Flyway.configure()
				.dataSource(dataSource)
				.locations("classpath:db/migration", "filesystem:src/main/resources/db/migration")
				.baselineOnMigrate(true)
				.load();
		
		// This forces execution, bypassing all Spring Boot auto-config rules
		flyway.migrate();
		return flyway;
	}
}