package com.bable.b_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class BBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BBackendApplication.class, args);
	}

}
