package com.parking.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDTO(
        UUID id,
        UUID recipientId,
        String channel,
        String subject,
        String body,
        LocalDateTime sentAt,
        String relatedEventType
) {}
