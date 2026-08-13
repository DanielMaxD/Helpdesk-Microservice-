package com.helpdesk.ticketservice.entity;

/**
 * Valid lifecycle: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED.
 * No other transitions are permitted (enforced in TicketService).
 */
public enum Status {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
