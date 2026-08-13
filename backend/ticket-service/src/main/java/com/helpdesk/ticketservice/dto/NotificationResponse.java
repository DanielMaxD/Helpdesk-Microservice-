package com.helpdesk.ticketservice.dto;

import com.helpdesk.ticketservice.entity.NotificationType;
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
public class NotificationResponse {

    private UUID id;
    private NotificationType type;
    private String message;
    private boolean read;
    private Instant createdAt;
}
