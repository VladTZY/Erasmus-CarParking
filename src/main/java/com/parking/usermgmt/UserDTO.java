package com.parking.usermgmt;

import java.util.UUID;

/** Read model returned to callers outside this module. */
public record UserDTO(UUID id, String email, User.Role role) {}
