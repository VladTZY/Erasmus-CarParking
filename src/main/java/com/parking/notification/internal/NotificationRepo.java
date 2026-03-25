package com.parking.notification.internal;

import com.parking.notification.INotificationRepo;
import com.parking.notification.NotificationDTO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class NotificationRepo implements INotificationRepo {

    private final NotificationJpaRepo jpa;

    NotificationRepo(NotificationJpaRepo jpa) {
        this.jpa = jpa;
    }

    Notification save(Notification notification) {
        return jpa.save(notification);
    }

    @Override
    public List<NotificationDTO> findByRecipientId(UUID recipientId) {
        return jpa.findByRecipientId(recipientId).stream().map(this::toDTO).toList();
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(n.getId(), n.getRecipientId(), n.getChannel().name(),
                n.getSubject(), n.getBody(), n.getSentAt(), n.getRelatedEventType());
    }
}
