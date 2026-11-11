package com.omnicall.call;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "translation_events")
public class TranslationEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID callId;

    @Column(nullable = false)
    private UUID speakerId;

    @Column(nullable = false, length = 16)
    private String sourceLanguage;

    @Column(nullable = false, length = 16)
    private String targetLanguage;

    @Column(nullable = false)
    private String originalText;

    private String translatedText;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    public static TranslationEvent create(
            UUID callId,
            UUID speakerId,
            String sourceLanguage,
            String targetLanguage,
            String originalText,
            String translatedText,
            String status) {
        TranslationEvent event = new TranslationEvent();
        event.id = UUID.randomUUID();
        event.callId = callId;
        event.speakerId = speakerId;
        event.sourceLanguage = sourceLanguage;
        event.targetLanguage = targetLanguage;
        event.originalText = originalText;
        event.translatedText = translatedText;
        event.status = status;
        event.createdAt = Instant.now();
        return event;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCallId() {
        return callId;
    }

    public UUID getSpeakerId() {
        return speakerId;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
