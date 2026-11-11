package com.omnicall.privacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_records")
public class ConsentRecord {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 40)
    private String consentType;

    @Column(nullable = false)
    private boolean granted;

    @Column(nullable = false, length = 20)
    private String policyVersion;

    @Column(nullable = false)
    private Instant createdAt;

    public static ConsentRecord of(UUID userId, String type, boolean granted, String version) {
        ConsentRecord record = new ConsentRecord();
        record.id = UUID.randomUUID();
        record.userId = userId;
        record.consentType = type;
        record.granted = granted;
        record.policyVersion = version;
        record.createdAt = Instant.now();
        return record;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getConsentType() {
        return consentType;
    }

    public boolean isGranted() {
        return granted;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
