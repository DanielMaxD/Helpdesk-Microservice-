package com.helpdesk.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import java.util.TimeZone;

/**
 * excludes UserDetailsServiceAutoConfiguration: this service authenticates
 * entirely through its own JWT filter (JwtAuthenticationFilter) and never uses
 * Spring Security's UserDetailsService/AuthenticationManager machinery. Without
 * this exclusion, Spring Boot silently registers an unused in-memory user and
 * logs a "Using generated security password" warning on every startup.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class UserServiceApplication {

    public static void main(String[] args) {
        // Force UTC as the JVM default timezone before any Spring context, JDBC
        // connection, or Hibernate session is created. This makes @CreationTimestamp
        // / @UpdateTimestamp and all Instant <-> Postgres timestamp conversions
        // deterministic regardless of the host machine's OS timezone (e.g.
        // Asia/Calcutta), permanently - no JAVA_TOOL_OPTIONS or -Duser.timezone
        // flag required on any machine that runs this jar.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        SpringApplication.run(UserServiceApplication.class, args);
    }
}
