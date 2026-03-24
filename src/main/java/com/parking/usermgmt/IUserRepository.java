package com.parking.usermgmt;

import java.util.Optional;

/**
 * Public repository contract for the User aggregate.
 * Exposes only the custom query method; standard CRUD is on the JPA implementation.
 * Other modules must not import the internal {@code UserRepository} directly.
 */
public interface IUserRepository {
    Optional<User> findByEmail(String email);
}
