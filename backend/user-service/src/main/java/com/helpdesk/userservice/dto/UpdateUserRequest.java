package com.helpdesk.userservice.dto;

import com.helpdesk.userservice.entity.Role;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * All fields optional — ADMIN performs a partial update.
 * Any field left null is left unchanged.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    private Role role;

    private Boolean active;
}
