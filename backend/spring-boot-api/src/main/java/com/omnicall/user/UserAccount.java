package com.omnicall.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, unique = true, length = 64)
    private String phoneNumber;

    @Column(nullable = false)
    private boolean phoneVerified;

    @Column(nullable = false, length = 16)
    private String preferredLanguage;

    @Column(length = 512)
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean translationConsent;

    @Column(nullable = false)
    private Instant termsAcceptedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant deletedAt;

    public static UserAccount create(String displayName, String phoneNumber, String preferredLanguage, boolean translationConsent) {
        UserAccount user = new UserAccount();
        user.id = UUID.randomUUID();
        user.displayName = displayName;
        user.phoneNumber = phoneNumber;
        user.phoneVerified = true;
        user.preferredLanguage = preferredLanguage;
        user.translationConsent = translationConsent;
        user.termsAcceptedAt = Instant.now();
        user.status = AccountStatus.ACTIVE;
        return user;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isTranslationConsent() {
        return translationConsent;
    }

    public void setTranslationConsent(boolean translationConsent) {
        this.translationConsent = translationConsent;
    }

    public Instant getTermsAcceptedAt() {
        return termsAcceptedAt;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
