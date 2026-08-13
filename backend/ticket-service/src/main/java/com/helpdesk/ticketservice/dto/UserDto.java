package com.helpdesk.ticketservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Local representation of user-service's UserResponse shape, used only for
 * deserializing responses from user-service. Role is kept as a plain String
 * (rather than referencing user-service's Role enum) to keep the two services
 * decoupled at compile time.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;
    private String name;
    private String email;
    private String role;
    private boolean active;
    private Instant createdAt;
}
