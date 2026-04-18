package com.hanplane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class HanplaneApplication {

	public static void main(String[] args) {
		SpringApplication.run(HanplaneApplication.class, args);
	}

}
