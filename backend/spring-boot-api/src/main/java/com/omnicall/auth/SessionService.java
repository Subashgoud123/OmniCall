package com.omnicall.auth;

import com.omnicall.config.OmniCallProperties;
import com.omnicall.security.JwtService;
import com.omnicall.user.UserAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private final StringRedisTemplate redis;
    private final JwtService jwtService;
    private final OmniCallProperties properties;
    private final SecureRandom random = new SecureRandom();

    public SessionService(StringRedisTemplate redis, JwtService jwtService, OmniCallProperties properties) {
        this.redis = redis;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    public AuthTokens issue(UserAccount user) {
        String access = jwtService.createAccessToken(user.getId(), user.getPhoneNumber());
        String refresh = newRefreshToken();
        storeRefresh(user.getId(), refresh);
        return new AuthTokens(access, refresh, user.getId());
    }

    public UUID requireRefresh(String refreshToken) {
        String userId = redis.opsForValue().get(refreshKey(refreshToken));
        if (userId == null) {
            throw new com.omnicall.common.ApiException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "SESSION_EXPIRED",
                    "Sign in again.");
        }
        return UUID.fromString(userId);
    }

    public void revokeRefresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        redis.delete(refreshKey(refreshToken));
    }

    private void storeRefresh(UUID userId, String refreshToken) {
        redis.opsForValue().set(refreshKey(refreshToken), userId.toString(), properties.getJwt().getRefreshTtl());
    }

    private String refreshKey(String refreshToken) {
        return "refresh:" + sha(refreshToken);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record AuthTokens(String accessToken, String refreshToken, UUID userId) {
    }
}
