package com.helpdesk.ticketservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AssignRequest {

    @NotNull(message = "agentId is required")
    private UUID agentId;
}
