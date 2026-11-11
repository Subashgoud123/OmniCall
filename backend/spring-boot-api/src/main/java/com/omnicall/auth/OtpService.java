package com.omnicall.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicall.common.ApiException;
import com.omnicall.config.OmniCallProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final OmniCallProperties properties;
    private final SecureRandom random = new SecureRandom();

    public OtpService(StringRedisTemplate redis, ObjectMapper objectMapper, OmniCallProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String issue(String phone, String purpose, Object payload) {
        String code = numericCode(properties.getOtp().getLength());
        Challenge challenge = new Challenge(hash(code), 0, write(payload));
        redis.opsForValue().set(key(phone, purpose), write(challenge), properties.getOtp().getTtl());
        return code;
    }

    public String consume(String phone, String purpose, String code) {
        String redisKey = key(phone, purpose);
        String raw = redis.opsForValue().get(redisKey);
        if (raw == null) {
            throw invalid();
        }
        Challenge challenge = read(raw, Challenge.class);
        if (challenge.attempts() >= properties.getOtp().getMaxAttempts()) {
            redis.delete(redisKey);
            throw invalid();
        }
        if (!MessageDigest.isEqual(hash(code).getBytes(StandardCharsets.UTF_8), challenge.hash().getBytes(StandardCharsets.UTF_8))) {
            Long ttl = redis.getExpire(redisKey);
            Duration remaining = ttl == null || ttl < 0 ? properties.getOtp().getTtl() : Duration.ofSeconds(ttl);
            redis.opsForValue().set(
                    redisKey,
                    write(new Challenge(challenge.hash(), challenge.attempts() + 1, challenge.payload())),
                    remaining);
            throw invalid();
        }
        redis.delete(redisKey);
        return challenge.payload();
    }

    private String numericCode(int length) {
        int bound = (int) Math.pow(10, length);
        int value = random.nextInt(bound);
        return String.format("%0" + length + "d", value);
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((code + properties.getJwt().getSecret()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String key(String phone, String purpose) {
        return "otp:" + purpose + ":" + phone;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw invalid();
        }
    }

    private ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "OTP_INVALID", "That code is incorrect or expired.");
    }

    public record Challenge(String hash, int attempts, String payload) {
    }
}
