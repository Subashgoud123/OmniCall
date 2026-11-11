package com.omnicall.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class AppNotification {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 40)
    private String notificationType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    public static AppNotification pending(UUID userId, String type, String payload) {
        AppNotification notification = new AppNotification();
        notification.id = UUID.randomUUID();
        notification.userId = userId;
        notification.notificationType = type;
        notification.status = "PENDING";
        notification.payload = payload;
        notification.createdAt = Instant.now();
        return notification;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
