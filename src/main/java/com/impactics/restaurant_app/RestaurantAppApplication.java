package com.impactics.restaurant_app;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RestaurantAppApplication {

	public static void main(String[] args) {
		runFlywayMigrations();
		SpringApplication.run(RestaurantAppApplication.class, args);
	}

	private static void runFlywayMigrations() {
		String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/fast_food");
		String dbUsername = System.getenv().getOrDefault("DB_USERNAME", "fastfood");
		String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "fastfood");

		Flyway flyway = Flyway.configure()
				.dataSource(dbUrl, dbUsername, dbPassword)
				.locations("classpath:db/migration")
				.baselineOnMigrate(true)
				.load();

		flyway.migrate();
	}

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Impactis Restaurant API")
						.version("1.0")
						.description("RESTful backend for the restaurant ordering platform."));
	}
}