package com.parking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.modulith.Modulithic;

// Suppress "generated security password" warning — JWT auth is used instead
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@Modulithic
public class ParkingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkingApplication.class, args);
    }
}
