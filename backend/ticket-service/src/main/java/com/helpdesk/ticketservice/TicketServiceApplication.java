package com.helpdesk.ticketservice;

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
public class TicketServiceApplication {

    public static void main(String[] args) {
        // Force UTC as the JVM default timezone before any Spring context, JDBC
        // connection, or Hibernate session is created. See UserServiceApplication
        // for the full rationale - both services must do this identically since
        // they share timestamp semantics across the API boundary.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
