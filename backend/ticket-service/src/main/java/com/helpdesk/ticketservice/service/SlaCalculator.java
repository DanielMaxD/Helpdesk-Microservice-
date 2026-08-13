package com.helpdesk.ticketservice.service;

import com.helpdesk.ticketservice.dto.SlaState;
import com.helpdesk.ticketservice.entity.Priority;
import com.helpdesk.ticketservice.entity.Status;
import com.helpdesk.ticketservice.entity.Ticket;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Pure, stateless SLA math - no background worker. States are computed on demand
 * whenever a ticket is read (TicketService.toResponse / getStatistics).
 */
@Component
public class SlaCalculator {

    private static final double AT_RISK_THRESHOLD = 0.25;

    public Instant calculateDueAt(Instant createdAt, Priority priority) {
        return createdAt.plus(priority.getSlaHours(), ChronoUnit.HOURS);
    }

    public SlaState calculateState(Ticket ticket, Instant now) {
        Instant dueAt = ticket.getDueAt();

        if (ticket.getStatus() == Status.RESOLVED || ticket.getStatus() == Status.CLOSED) {
            Instant reference = ticket.getResolvedAt() != null ? ticket.getResolvedAt() : ticket.getUpdatedAt();
            return reference.isAfter(dueAt) ? SlaState.BREACHED : SlaState.ON_TRACK;
        }

        if (now.isAfter(dueAt)) {
            return SlaState.BREACHED;
        }

        Instant createdAt = ticket.getCreatedAt();
        long totalMillis = dueAt.toEpochMilli() - createdAt.toEpochMilli();
        long remainingMillis = dueAt.toEpochMilli() - now.toEpochMilli();

        if (totalMillis <= 0) {
            return SlaState.AT_RISK;
        }

        double remainingFraction = (double) remainingMillis / (double) totalMillis;
        return remainingFraction <= AT_RISK_THRESHOLD ? SlaState.AT_RISK : SlaState.ON_TRACK;
    }
}
