package com.parking.notification;

import java.util.UUID;

public record NotificationSentEvent(
        UUID notificationId,
        UUID recipientId,
        String eventType
) {}
