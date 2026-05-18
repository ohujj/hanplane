package com.hanplane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class HanplaneApplication {

	public static void main(String[] args) {
		SpringApplication.run(HanplaneApplication.class, args);
		
	}

}
