package com.omnicall.auth;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {

    private final StringRedisTemplate redis;

    public AuthRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void assertOtpAllowed(String phone) {
        String key = "otp:rate:" + phone;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofHours(1));
        }
        if (count != null && count > 5) {
            throw new com.omnicall.common.ApiException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "OTP_RATE_LIMIT",
                    "Too many verification attempts. Wait before requesting another code.");
        }
    }
}
