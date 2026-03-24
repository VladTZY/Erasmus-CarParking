package com.parking.usermgmt.internal;

import com.parking.usermgmt.User;
import com.parking.usermgmt.UserDTO;
import com.parking.usermgmt.UserRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
class UserService {
    

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    UserService(UserRepository userRepository,
                PasswordEncoder passwordEncoder,
                ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    UserDTO register(String email, String rawPassword, User.Role role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email already registered: " + email);
        }
        var user = new User(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(rawPassword),
                role,
                LocalDateTime.now()
        );
        user = userRepository.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail(), user.getRole()));
        return toDTO(user);
    }

    @Transactional(readOnly = true)
    UserDTO findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    @Transactional(readOnly = true)
    UserDTO findById(UUID id) {
        return userRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional
    UserDTO updateProfile(UUID userId, String newEmail) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setEmail(newEmail);
        return toDTO(userRepository.save(user));
    }

    private UserDTO toDTO(User u) {
        return new UserDTO(u.getId(), u.getEmail(), u.getRole());
    }
}
