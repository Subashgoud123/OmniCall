package com.omnicall.call;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UsageLedger {

    private final StringRedisTemplate redis;
    private final com.omnicall.config.OmniCallProperties properties;

    public UsageLedger(StringRedisTemplate redis, com.omnicall.config.OmniCallProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void assertCanStart(UUID userId) {
        long used = usedSeconds(userId);
        if (used >= properties.getUsage().getDailyCallSeconds()) {
            throw new com.omnicall.common.ApiException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "DAILY_LIMIT",
                    "Today's translated-call limit has been reached.");
        }
    }

    public void add(UUID userId, long seconds) {
        if (seconds <= 0) {
            return;
        }
        String key = key(userId);
        redis.opsForValue().increment(key, seconds);
        redis.expire(key, Duration.ofDays(2));
    }

    private long usedSeconds(UUID userId) {
        String raw = redis.opsForValue().get(key(userId));
        if (raw == null) {
            return 0;
        }
        return Long.parseLong(raw);
    }

    private String key(UUID userId) {
        return "usage:" + userId + ":" + LocalDate.now();
    }
}
