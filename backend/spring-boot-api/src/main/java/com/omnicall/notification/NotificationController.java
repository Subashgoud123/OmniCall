package com.omnicall.notification;

import com.omnicall.security.AuthContext;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private final DeviceTokenRepository devices;
    private final NotificationRepository notifications;

    public NotificationController(DeviceTokenRepository devices, NotificationRepository notifications) {
        this.devices = devices;
        this.notifications = notifications;
    }

    @PostMapping("/api/devices")
    public Map<String, String> register(@RequestBody DeviceRequest request) {
        UUID userId = AuthContext.current().id();
        DeviceToken existing = devices.findByToken(request.token()).orElse(null);
        if (existing == null) {
            devices.save(DeviceToken.create(userId, request.token(), request.platform()));
        } else {
            existing.setUserId(userId);
            existing.setPlatform(request.platform());
            existing.setUpdatedAt(Instant.now());
            devices.save(existing);
        }
        return Map.of("status", "REGISTERED");
    }

    @GetMapping("/api/notifications")
    public List<NotificationView> list() {
        return notifications.findByUserIdOrderByCreatedAtDesc(AuthContext.current().id()).stream()
                .map(item -> new NotificationView(item.getId(), item.getNotificationType(), item.getStatus(), item.getPayload(), item.getCreatedAt()))
                .toList();
    }

    @PostMapping("/api/notifications/{id}/read")
    public Map<String, String> read(@PathVariable UUID id) {
        AppNotification notification = notifications.findById(id)
                .filter(item -> item.getUserId().equals(AuthContext.current().id()))
                .orElseThrow(() -> new com.omnicall.common.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND",
                        "That notification does not exist."));
        notification.setStatus("READ");
        notifications.save(notification);
        return Map.of("status", "READ");
    }

    public record DeviceRequest(@NotBlank String token, @NotBlank String platform) {
    }

    public record NotificationView(UUID id, String type, String status, String payload, Instant createdAt) {
    }
}
