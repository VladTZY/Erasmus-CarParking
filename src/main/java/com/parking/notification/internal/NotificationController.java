package com.parking.notification.internal;

import com.parking.notification.NotificationDTO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
class NotificationController {

    private final NotificationRepo notificationRepo;

    NotificationController(NotificationRepo notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @GetMapping("/notifications/my")
    List<NotificationDTO> getMy(Authentication auth) {
        UUID recipientId = (UUID) auth.getPrincipal();
        return notificationRepo.findByRecipientId(recipientId);
    }
}
