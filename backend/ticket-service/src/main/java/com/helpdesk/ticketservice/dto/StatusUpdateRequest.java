package com.helpdesk.ticketservice.dto;

import com.helpdesk.ticketservice.entity.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StatusUpdateRequest {

    @NotNull(message = "Status is required")
    private Status status;
}
