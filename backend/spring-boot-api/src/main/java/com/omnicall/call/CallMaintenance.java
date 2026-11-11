package com.omnicall.call;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CallMaintenance {

    private final CallService calls;
    private final TranslationEventRepository translations;
    private final com.omnicall.config.OmniCallProperties properties;

    public CallMaintenance(CallService calls, TranslationEventRepository translations, com.omnicall.config.OmniCallProperties properties) {
        this.calls = calls;
        this.translations = translations;
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 15000)
    public void expireRingingCalls() {
        calls.expireRinging();
    }

    @Scheduled(cron = "0 30 3 * * *")
    @org.springframework.transaction.annotation.Transactional
    public void purgeTranslationHistory() {
        java.time.Instant cutoff = java.time.Instant.now().minus(java.time.Duration.ofDays(properties.getPrivacy().getTranslationRetentionDays()));
        translations.deleteOlderThan(cutoff);
    }
}
