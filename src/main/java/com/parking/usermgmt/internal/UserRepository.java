package com.parking.usermgmt.internal;

import com.parking.usermgmt.IUserRepository;
import com.parking.usermgmt.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA implementation of the module's repository contract.
 * Package-private — callers outside this module use {@link IUserRepository}.
 */
interface UserRepository extends JpaRepository<User, UUID>, IUserRepository {
    // findByEmail(String) — satisfied by Spring Data query derivation
    // findById(UUID)      — satisfied by CrudRepository
}
