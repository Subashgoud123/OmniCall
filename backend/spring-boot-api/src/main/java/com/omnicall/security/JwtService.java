package com.omnicall.security;

import com.omnicall.config.OmniCallProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final OmniCallProperties properties;
    private final SecretKey key;

    public JwtService(OmniCallProperties properties) {
        this.properties = properties;
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes.");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String createAccessToken(UUID userId, String phone) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getJwt().getAccessTtl());
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("phone", phone)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public AccessClaims parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AccessClaims(UUID.fromString(claims.getSubject()), claims.getId(), claims.getExpiration().toInstant());
    }

    public record AccessClaims(UUID userId, String jti, Instant expiresAt) {
    }
}
