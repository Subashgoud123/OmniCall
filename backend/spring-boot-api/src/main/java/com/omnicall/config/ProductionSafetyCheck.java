package com.omnicall.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ProductionSafetyCheck {

    @Bean
    ApplicationRunner rejectUnsafeProductionSettings(Environment environment, OmniCallProperties properties) {
        return args -> {
            boolean production = environment.matchesProfiles("prod");
            if (!production) {
                return;
            }
            if ("log".equalsIgnoreCase(properties.getSms().getProvider())) {
                throw new IllegalStateException("SMS_PROVIDER=log cannot be used in production.");
            }
            if (properties.getJwt().getSecret().contains("change-me")) {
                throw new IllegalStateException("Replace JWT_SECRET before production.");
            }
            if (properties.getInternalToken().contains("dev-internal")) {
                throw new IllegalStateException("Replace INTERNAL_API_TOKEN before production.");
            }
            if ("secret".equals(properties.getLivekit().getApiSecret())) {
                throw new IllegalStateException("Replace the LiveKit API secret before production.");
            }
        };
    }
}
