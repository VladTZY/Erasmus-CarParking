package com.parking.usermgmt;

import java.util.UUID;

/**
 * Published when a new user completes registration.
 * The Notification module listens to this event to send a welcome e-mail.
 */
public record UserRegisteredEvent(UUID userId, String email, User.Role role) {}
