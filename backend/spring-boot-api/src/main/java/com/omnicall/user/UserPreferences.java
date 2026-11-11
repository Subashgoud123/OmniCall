package com.omnicall.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(length = 80)
    private String preferredVoice;

    @Column(nullable = false, length = 16)
    private String voiceGender;

    @Column(nullable = false)
    private boolean subtitleEnabled;

    @Column(nullable = false)
    private boolean originalAudioEnabled;

    @Column(nullable = false)
    private double translationSpeed;

    @Column(nullable = false)
    private boolean notificationsEnabled;

    @Column(nullable = false)
    private boolean translationHistoryEnabled;

    public static UserPreferences defaults(UUID userId) {
        UserPreferences preferences = new UserPreferences();
        preferences.id = UUID.randomUUID();
        preferences.userId = userId;
        preferences.voiceGender = "FEMALE";
        preferences.subtitleEnabled = true;
        preferences.originalAudioEnabled = false;
        preferences.translationSpeed = 1.0;
        preferences.notificationsEnabled = true;
        preferences.translationHistoryEnabled = false;
        return preferences;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPreferredVoice() {
        return preferredVoice;
    }

    public void setPreferredVoice(String preferredVoice) {
        this.preferredVoice = preferredVoice;
    }

    public String getVoiceGender() {
        return voiceGender;
    }

    public void setVoiceGender(String voiceGender) {
        this.voiceGender = voiceGender;
    }

    public boolean isSubtitleEnabled() {
        return subtitleEnabled;
    }

    public void setSubtitleEnabled(boolean subtitleEnabled) {
        this.subtitleEnabled = subtitleEnabled;
    }

    public boolean isOriginalAudioEnabled() {
        return originalAudioEnabled;
    }

    public void setOriginalAudioEnabled(boolean originalAudioEnabled) {
        this.originalAudioEnabled = originalAudioEnabled;
    }

    public double getTranslationSpeed() {
        return translationSpeed;
    }

    public void setTranslationSpeed(double translationSpeed) {
        this.translationSpeed = translationSpeed;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean isTranslationHistoryEnabled() {
        return translationHistoryEnabled;
    }

    public void setTranslationHistoryEnabled(boolean translationHistoryEnabled) {
        this.translationHistoryEnabled = translationHistoryEnabled;
    }
}
