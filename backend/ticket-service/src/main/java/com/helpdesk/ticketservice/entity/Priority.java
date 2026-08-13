package com.helpdesk.ticketservice.entity;

/**
 * SLA response-time window (in hours) associated with each priority level.
 */
public enum Priority {
    LOW(24),
    MEDIUM(8),
    HIGH(4),
    CRITICAL(1);

    private final int slaHours;

    Priority(int slaHours) {
        this.slaHours = slaHours;
    }

    public int getSlaHours() {
        return slaHours;
    }
}
