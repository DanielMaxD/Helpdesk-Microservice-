package com.helpdesk.ticketservice.dto;

import com.helpdesk.ticketservice.entity.Category;
import com.helpdesk.ticketservice.entity.Priority;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * All fields optional - the caller (ADMIN, or the assigned AGENT) performs a
 * partial update. Any field left null is left unchanged.
 */
@Getter
@Setter
@NoArgsConstructor
public class TicketUpdateRequest {

    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    private String description;

    private Priority priority;

    private Category category;
}
