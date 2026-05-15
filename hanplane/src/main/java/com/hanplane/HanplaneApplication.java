package com.hanplane;

import com.hanplane.domain.coupon.service.CouponSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@EnableJpaAuditing
@SpringBootApplication
public class HanplaneApplication {

	public static void main(String[] args) {
		SpringApplication.run(HanplaneApplication.class, args);
		
	}

}
