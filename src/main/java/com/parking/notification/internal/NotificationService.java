package com.parking.notification.internal;

import com.parking.notification.NotificationSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepo repo;
    private final ApplicationEventPublisher eventPublisher;

    NotificationService(NotificationRepo repo, ApplicationEventPublisher eventPublisher) {
        this.repo = repo;
        this.eventPublisher = eventPublisher;
    }

    void send(UUID recipientId, String subject, String body, String eventType) {
        log.info("NOTIFICATION → recipient={} subject={} body={}", recipientId, subject, body);

        var notification = new Notification(
                UUID.randomUUID(), recipientId, NotificationChannel.LOG,
                subject, body, LocalDateTime.now(), eventType
        );
        notification = repo.save(notification);

        eventPublisher.publishEvent(new NotificationSentEvent(
                notification.getId(), recipientId, eventType
        ));
    }
}
