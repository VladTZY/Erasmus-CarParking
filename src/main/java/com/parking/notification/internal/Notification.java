package com.parking.notification.internal;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
class Notification {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private String relatedEventType;

    protected Notification() {}

    Notification(UUID id, UUID recipientId, NotificationChannel channel,
                 String subject, String body, LocalDateTime sentAt, String relatedEventType) {
        this.id = id;
        this.recipientId = recipientId;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.sentAt = sentAt;
        this.relatedEventType = relatedEventType;
    }

    UUID getId() { return id; }
    UUID getRecipientId() { return recipientId; }
    NotificationChannel getChannel() { return channel; }
    String getSubject() { return subject; }
    String getBody() { return body; }
    LocalDateTime getSentAt() { return sentAt; }
    String getRelatedEventType() { return relatedEventType; }
}
