package com.helpdesk.ticketservice.service;

import com.helpdesk.ticketservice.dto.SlaState;
import com.helpdesk.ticketservice.entity.Priority;
import com.helpdesk.ticketservice.entity.Status;
import com.helpdesk.ticketservice.entity.Ticket;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlaCalculatorTest {

    private final SlaCalculator slaCalculator = new SlaCalculator();

    @Test
    void calculateDueAtAddsPrioritySlaHours() {
        Instant createdAt = Instant.parse("2026-08-10T10:00:00Z");

        assertEquals(createdAt.plus(1, ChronoUnit.HOURS), slaCalculator.calculateDueAt(createdAt, Priority.CRITICAL));
        assertEquals(createdAt.plus(4, ChronoUnit.HOURS), slaCalculator.calculateDueAt(createdAt, Priority.HIGH));
        assertEquals(createdAt.plus(8, ChronoUnit.HOURS), slaCalculator.calculateDueAt(createdAt, Priority.MEDIUM));
        assertEquals(createdAt.plus(24, ChronoUnit.HOURS), slaCalculator.calculateDueAt(createdAt, Priority.LOW));
    }

    @Test
    void activeTicketWithPlentyOfTimeIsOnTrack() {
        Instant now = Instant.parse("2026-08-10T12:00:00Z");
        Instant createdAt = now.minus(1, ChronoUnit.HOURS); // 1h ago
        Instant dueAt = createdAt.plus(8, ChronoUnit.HOURS); // MEDIUM window, 7h remaining of 8h -> 87% remains

        Ticket ticket = ticket(Status.OPEN, createdAt, dueAt, null);

        assertEquals(SlaState.ON_TRACK, slaCalculator.calculateState(ticket, now));
    }

    @Test
    void activeTicketWithLessThan25PercentRemainingIsAtRisk() {
        Instant now = Instant.parse("2026-08-10T12:00:00Z");
        Instant createdAt = now.minus(7, ChronoUnit.HOURS); // 7h ago
        Instant dueAt = createdAt.plus(8, ChronoUnit.HOURS); // MEDIUM window, 1h remaining of 8h -> 12.5% remains

        Ticket ticket = ticket(Status.IN_PROGRESS, createdAt, dueAt, null);

        assertEquals(SlaState.AT_RISK, slaCalculator.calculateState(ticket, now));
    }

    @Test
    void activeTicketPastDueDateIsBreached() {
        Instant now = Instant.parse("2026-08-10T12:00:00Z");
        Instant createdAt = now.minus(10, ChronoUnit.HOURS);
        Instant dueAt = now.minus(1, ChronoUnit.HOURS); // already passed

        Ticket ticket = ticket(Status.IN_PROGRESS, createdAt, dueAt, null);

        assertEquals(SlaState.BREACHED, slaCalculator.calculateState(ticket, now));
    }

    @Test
    void resolvedBeforeDueDateIsOnTrack() {
        Instant now = Instant.parse("2026-08-10T12:00:00Z");
        Instant createdAt = now.minus(10, ChronoUnit.HOURS);
        Instant dueAt = now.minus(2, ChronoUnit.HOURS);
        Instant resolvedAt = now.minus(5, ChronoUnit.HOURS); // resolved before dueAt

        Ticket ticket = ticket(Status.RESOLVED, createdAt, dueAt, resolvedAt);

        assertEquals(SlaState.ON_TRACK, slaCalculator.calculateState(ticket, now));
    }

    @Test
    void resolvedAfterDueDateIsBreached() {
        Instant now = Instant.parse("2026-08-10T12:00:00Z");
        Instant createdAt = now.minus(10, ChronoUnit.HOURS);
        Instant dueAt = now.minus(8, ChronoUnit.HOURS);
        Instant resolvedAt = now.minus(1, ChronoUnit.HOURS); // resolved after dueAt

        Ticket ticket = ticket(Status.RESOLVED, createdAt, dueAt, resolvedAt);

        assertEquals(SlaState.BREACHED, slaCalculator.calculateState(ticket, now));
    }

    private Ticket ticket(Status status, Instant createdAt, Instant dueAt, Instant resolvedAt) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .title("Test ticket")
                .description("Test description")
                .priority(Priority.MEDIUM)
                .category(com.helpdesk.ticketservice.entity.Category.OTHER)
                .status(status)
                .createdBy(UUID.randomUUID())
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .dueAt(dueAt)
                .resolvedAt(resolvedAt)
                .build();
    }
}
