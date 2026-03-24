package com.parking.usermgmt.internal;

import com.parking.usermgmt.User;
import com.parking.usermgmt.UserDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
class UserController {

    private final UserService userService;
    private final AuthService authService;

    UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    // ── Endpoints ────────────────────────────────────────────────────────────

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserDTO register(@RequestBody @Valid RegisterRequest request) {
        return userService.register(request.email(), request.password(), request.role());
    }

    @PostMapping("/auth/login")
    Map<String, String> login(@RequestBody @Valid LoginRequest request) {
        String token = authService.login(request.email(), request.password());
        return Map.of("token", token);
    }

    @GetMapping("/users/me")
    UserDTO me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return userService.findById(userId);
    }

    // ── Error handlers ───────────────────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // ── Request records ──────────────────────────────────────────────────────

    record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotNull User.Role role
    ) {}

    record LoginRequest(
            @NotBlank String email,
            @NotBlank String password
    ) {}
}
