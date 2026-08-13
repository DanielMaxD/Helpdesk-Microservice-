package com.helpdesk.ticketservice.dto;

import com.helpdesk.ticketservice.entity.Category;
import com.helpdesk.ticketservice.entity.Priority;
import com.helpdesk.ticketservice.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private UUID id;
    private String title;
    private String description;
    private Priority priority;
    private Status status;
    private Category category;
    private UUID createdBy;
    private UUID assignedAgent;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
    private Instant dueAt;
    private SlaState slaState;
}
