package com.parking.usermgmt.internal;

import com.parking.usermgmt.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@Profile("!test")
class UserDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserDataSeeder.class);

    private final UserService userService;
    private final UserRepository userRepository;

    UserDataSeeder(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@parking.city").isPresent()) {
            log.info("[Seed] Users already present — skipping user seed");
            return;
        }

        var admin = userService.register("admin@parking.city", "password123", User.Role.ADMIN);
        var alice = userService.register("alice@example.com", "password123", User.Role.CITIZEN);
        var bob   = userService.register("bob@example.com",   "password123", User.Role.CITIZEN);

        log.info("[Seed] Users created:");
        log.info("[Seed]   admin  id={} email={}", admin.id(), admin.email());
        log.info("[Seed]   alice  id={} email={}", alice.id(), alice.email());
        log.info("[Seed]   bob    id={} email={}", bob.id(),   bob.email());
    }
}
