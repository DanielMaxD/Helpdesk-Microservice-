package com.helpdesk.ticketservice.config;

import com.helpdesk.ticketservice.client.UserServiceClient;
import com.helpdesk.ticketservice.dto.UserDto;
import com.helpdesk.ticketservice.entity.Category;
import com.helpdesk.ticketservice.entity.Notification;
import com.helpdesk.ticketservice.entity.NotificationType;
import com.helpdesk.ticketservice.entity.Priority;
import com.helpdesk.ticketservice.entity.Status;
import com.helpdesk.ticketservice.entity.Ticket;
import com.helpdesk.ticketservice.repository.NotificationRepository;
import com.helpdesk.ticketservice.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seeds demo tickets on first boot only (skipped if the tickets table is not empty).
 *
 * ticket-service does NOT share a database with user-service, and demo user UUIDs
 * are generated randomly by user-service at its own boot time - so this seeder
 * cannot hardcode them. Instead it authenticates against user-service's real,
 * public /api/auth/login endpoint using the known demo admin credentials, then
 * calls GET /api/users with that token to discover the actual UUIDs of the
 * seeded demo accounts. This guarantees every seeded ticket references a user
 * that genuinely exists in user-service - never a fabricated cross-service id.
 *
 * If user-service is unreachable or has not seeded its demo accounts yet, this
 * seeder logs a warning and skips ticket seeding gracefully; it never prevents
 * ticket-service itself from starting.
 */
@Component
public class TicketDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketDataSeeder.class);

    private static final String ADMIN_EMAIL = "admin@helpdesk.dev";
    private static final String DEMO_PASSWORD = "Password123!";

    private final TicketRepository ticketRepository;
    private final NotificationRepository notificationRepository;
    private final UserServiceClient userServiceClient;

    public TicketDataSeeder(
            TicketRepository ticketRepository,
            NotificationRepository notificationRepository,
            UserServiceClient userServiceClient
    ) {
        this.ticketRepository = ticketRepository;
        this.notificationRepository = notificationRepository;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public void run(String... args) {
        if (ticketRepository.count() > 0) {
            return;
        }

        Optional<String> token = userServiceClient.login(ADMIN_EMAIL, DEMO_PASSWORD);
        if (token.isEmpty()) {
            log.warn("Could not reach user-service (or its demo admin account) to seed demo tickets. "
                    + "Start user-service first, let it seed its demo accounts, then restart ticket-service "
                    + "to seed demo tickets.");
            return;
        }

        String bearer = "Bearer " + token.get();
        List<UserDto> users = userServiceClient.listAllUsers(bearer);
        Map<String, UserDto> byEmail = users.stream()
                .collect(Collectors.toMap(UserDto::getEmail, u -> u));

        UserDto agent1 = byEmail.get("agent1@helpdesk.dev");
        UserDto agent2 = byEmail.get("agent2@helpdesk.dev");
        UserDto user1 = byEmail.get("user1@helpdesk.dev");
        UserDto user2 = byEmail.get("user2@helpdesk.dev");
        UserDto user3 = byEmail.get("user3@helpdesk.dev");

        if (agent1 == null || agent2 == null || user1 == null || user2 == null || user3 == null) {
            log.warn("user-service demo accounts were not found (expected admin, agent1, agent2, "
                    + "user1, user2, user3). Skipping ticket seed data.");
            return;
        }

        Instant now = Instant.now();

        seedTicket("Cannot log into my account",
                "I keep getting an invalid password error even after resetting it twice today.",
                Priority.HIGH, Category.ACCOUNT, Status.OPEN,
                user1.getId(), null,
                now.plus(3, ChronoUnit.HOURS), null);

        seedTicket("Payment failed but amount was deducted",
                "My card was charged for the annual plan but the order still shows as unpaid.",
                Priority.CRITICAL, Category.PAYMENT, Status.OPEN,
                user2.getId(), null,
                now.minus(2, ChronoUnit.HOURS), null); // already past due -> BREACHED

        seedTicket("Question about enterprise pricing",
                "Could someone walk me through the enterprise pricing tiers and seat limits?",
                Priority.LOW, Category.OTHER, Status.OPEN,
                user3.getId(), null,
                now.plus(20, ChronoUnit.HOURS), null);

        seedTicket("App crashes on file upload",
                "The mobile app closes unexpectedly every time I try to attach a PDF over 5MB.",
                Priority.HIGH, Category.TECHNICAL, Status.IN_PROGRESS,
                user1.getId(), agent1.getId(),
                now.plus(3, ChronoUnit.HOURS), null);

        seedTicket("Incorrect invoice amount",
                "This month's invoice shows an extra line item that doesn't match my current plan.",
                Priority.MEDIUM, Category.BILLING, Status.IN_PROGRESS,
                user2.getId(), agent2.getId(),
                now.plus(6, ChronoUnit.HOURS), null);

        seedTicket("Slow dashboard loading",
                "The analytics dashboard has been taking over 10 seconds to load for the past two days.",
                Priority.MEDIUM, Category.TECHNICAL, Status.IN_PROGRESS,
                user3.getId(), agent1.getId(),
                now.plus(30, ChronoUnit.MINUTES), null); // <25% of 8h window left -> AT_RISK

        seedTicket("Two-factor authentication not working",
                "I'm not receiving the 2FA code by email when I try to sign in.",
                Priority.HIGH, Category.ACCOUNT, Status.RESOLVED,
                user1.getId(), agent2.getId(),
                now.minus(20, ChronoUnit.HOURS), now.minus(22, ChronoUnit.HOURS)); // resolved before due -> ON_TRACK

        seedTicket("Refund request for duplicate charge",
                "I was charged twice for the same monthly subscription on the same day.",
                Priority.CRITICAL, Category.PAYMENT, Status.RESOLVED,
                user2.getId(), agent1.getId(),
                now.minus(47, ChronoUnit.HOURS), now.minus(40, ChronoUnit.HOURS)); // resolved after due -> BREACHED

        seedTicket("Update billing address",
                "Please update my billing address to the one already on file with support.",
                Priority.LOW, Category.BILLING, Status.RESOLVED,
                user3.getId(), agent2.getId(),
                now.minus(48, ChronoUnit.HOURS), now.minus(52, ChronoUnit.HOURS)); // resolved before due -> ON_TRACK

        seedTicket("Password reset email not arriving",
                "I never received the password reset link after three separate attempts.",
                Priority.MEDIUM, Category.ACCOUNT, Status.CLOSED,
                user1.getId(), agent1.getId(),
                now.minus(112, ChronoUnit.HOURS), now.minus(114, ChronoUnit.HOURS));

        seedTicket("Export to CSV is broken",
                "Exporting my usage report to CSV produces a file with headers but no rows.",
                Priority.HIGH, Category.TECHNICAL, Status.CLOSED,
                user2.getId(), agent2.getId(),
                now.minus(140, ChronoUnit.HOURS), now.minus(142, ChronoUnit.HOURS));

        seedTicket("Cannot download previous invoices",
                "The invoice history page returns a 404 when I try to download anything older than last month.",
                Priority.LOW, Category.BILLING, Status.OPEN,
                user3.getId(), null,
                now.plus(22, ChronoUnit.HOURS), null);

        notificationRepository.save(Notification.builder()
                .userId(agent1.getId())
                .type(NotificationType.TICKET_ASSIGNED)
                .message("You have been assigned to ticket \"App crashes on file upload\".")
                .read(false)
                .build());

        notificationRepository.save(Notification.builder()
                .userId(user1.getId())
                .type(NotificationType.TICKET_RESOLVED)
                .message("Your ticket \"Two-factor authentication not working\" has been resolved.")
                .read(false)
                .build());

        log.info("Seeded {} demo tickets referencing real user-service accounts.", ticketRepository.count());
    }

    private void seedTicket(
            String title,
            String description,
            Priority priority,
            Category category,
            Status status,
            UUID createdBy,
            UUID assignedAgent,
            Instant dueAt,
            Instant resolvedAt
    ) {
        Ticket ticket = Ticket.builder()
                .title(title)
                .description(description)
                .priority(priority)
                .category(category)
                .status(status)
                .createdBy(createdBy)
                .assignedAgent(assignedAgent)
                .dueAt(dueAt)
                .resolvedAt(resolvedAt)
                .build();
        ticketRepository.save(ticket);
    }
}
