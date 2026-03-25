package com.parking.notification;

import java.util.List;
import java.util.UUID;

/**
 * Exported interface — allows other modules to query notification history
 * without depending on internal notification classes.
 */
public interface INotificationRepo {
    List<NotificationDTO> findByRecipientId(UUID recipientId);
}
