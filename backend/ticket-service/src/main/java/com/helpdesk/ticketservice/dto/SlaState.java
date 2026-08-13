package com.helpdesk.ticketservice.dto;

/**
 * Computed on read (no background worker) by SlaCalculator.
 * ON_TRACK  - more than 25% of the SLA window remains
 * AT_RISK   - 25% or less remains, but dueAt has not passed
 * BREACHED  - dueAt has passed (or the ticket was resolved/closed after dueAt)
 */
public enum SlaState {
    ON_TRACK,
    AT_RISK,
    BREACHED
}
