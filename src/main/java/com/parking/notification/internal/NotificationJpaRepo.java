package com.parking.notification.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface NotificationJpaRepo extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientId(UUID recipientId);
}
