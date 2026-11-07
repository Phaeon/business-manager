package com.businessmanager.dto.notification;

import com.businessmanager.entity.NotificationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class NotificationDtos {

    public record NotificationResponse(
            UUID id,
            NotificationType type,
            String message,
            String link,
            boolean read,
            Instant createdAt
    ) {
    }

    public record NotificationsPayload(List<NotificationResponse> items, long unreadCount) {
    }
}
