/**
 * User Management module.
 *
 * <p>Responsible for citizen and admin registration, authentication, and JWT issuance.
 * Published events: {@code UserRegisteredEvent}.
 * Exported interfaces: none (all auth interaction goes via REST endpoints).
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "User Management"
)
package com.parking.usermgmt;
