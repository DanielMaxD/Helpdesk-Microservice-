package com.helpdesk.ticketservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Scope depends on the caller's role (enforced in TicketService.getStatistics):
 * ADMIN sees all tickets, AGENT sees only tickets assigned to them,
 * USER sees only tickets they created.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketStatisticsResponse {

    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long closedTickets;

    /** Count of OPEN or IN_PROGRESS tickets whose SLA is currently BREACHED. */
    private long breachedTickets;
}
