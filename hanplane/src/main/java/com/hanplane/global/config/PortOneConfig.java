package com.hanplane.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.portone.sdk.server.PortOneClient;


@Configuration
public class PortOneConfig {

    @Value("${portone.secret}")
    private String apiSecret;

    @Value("${portone.store-id}")
    private String storeId;

    @Bean
    public PortOneClient portOneClient() {
        return new PortOneClient(apiSecret, storeId, "https://api.portone.io");
    }
}
