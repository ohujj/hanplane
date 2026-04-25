package com.hanplane;

import com.hanplane.domain.coupon.service.CouponSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@RequiredArgsConstructor
public class HanplaneApplication {

	private final CouponSyncService couponSyncService;

	public static void main(String[] args) {
		SpringApplication.run(HanplaneApplication.class, args);
		
	}

	@Bean
	public ApplicationRunner applicationRunner() {
		return args -> {
			couponSyncService.syncAll();
		};
	}

}
