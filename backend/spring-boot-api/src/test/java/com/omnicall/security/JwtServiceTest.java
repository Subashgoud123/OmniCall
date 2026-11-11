package com.omnicall.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.omnicall.config.OmniCallProperties;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void roundTripsTheUserId() {
        OmniCallProperties properties = new OmniCallProperties();
        properties.getJwt().setSecret("test-secret-that-is-at-least-32-bytes");
        properties.getJwt().setAccessTtl(Duration.ofMinutes(5));
        JwtService jwt = new JwtService(properties);
        UUID userId = UUID.randomUUID();
        String token = jwt.createAccessToken(userId, "+919876543210");
        assertEquals(userId, jwt.parse(token).userId());
        assertNotEquals(token, jwt.createAccessToken(userId, "+919876543210"));
    }
}
