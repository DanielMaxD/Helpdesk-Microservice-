package com.helpdesk.userservice.config;

import com.helpdesk.userservice.entity.Role;
import com.helpdesk.userservice.entity.User;
import com.helpdesk.userservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo data on first boot only (skipped if the users table is not empty).
 * All demo accounts share the password: Password123!
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        String defaultPassword = passwordEncoder.encode("Password123!");

        userRepository.save(User.builder()
                .name("Alice Admin")
                .email("admin@helpdesk.dev")
                .passwordHash(defaultPassword)
                .role(Role.ADMIN)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .name("Gina Agent")
                .email("agent1@helpdesk.dev")
                .passwordHash(defaultPassword)
                .role(Role.AGENT)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .name("Sam Support")
                .email("agent2@helpdesk.dev")
                .passwordHash(defaultPassword)
                .role(Role.AGENT)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .name("Uma User")
                .email("user1@helpdesk.dev")
                .passwordHash(defaultPassword)
                .role(Role.USER)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .name("Victor Visitor")
                .email("user2@helpdesk.dev")
                .passwordHash(defaultPassword)
                .role(Role.USER)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .name("Nina Newcomer")
                .email("user3@helpdesk.dev")
                .passwordHash(defaultPassword)
                .role(Role.USER)
                .active(true)
                .build());
    }
}
