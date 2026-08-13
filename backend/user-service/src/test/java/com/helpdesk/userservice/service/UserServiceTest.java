package com.helpdesk.userservice.service;

import com.helpdesk.userservice.dto.UpdateUserRequest;
import com.helpdesk.userservice.entity.Role;
import com.helpdesk.userservice.entity.User;
import com.helpdesk.userservice.exception.BadRequestException;
import com.helpdesk.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void adminCannotRemoveOwnAdminRole() {
        UUID adminId = UUID.randomUUID();
        User admin = User.builder()
                .id(adminId)
                .name("Alice Admin")
                .email("admin@helpdesk.dev")
                .role(Role.ADMIN)
                .active(true)
                .build();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole(Role.USER);

        assertThrows(BadRequestException.class, () -> userService.updateUser(adminId, request, adminId));
    }

    @Test
    void adminCannotDeactivateOwnAccount() {
        UUID adminId = UUID.randomUUID();
        User admin = User.builder()
                .id(adminId)
                .name("Alice Admin")
                .email("admin@helpdesk.dev")
                .role(Role.ADMIN)
                .active(true)
                .build();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setActive(false);

        assertThrows(BadRequestException.class, () -> userService.updateUser(adminId, request, adminId));
    }

    @Test
    void adminCannotDeleteOwnAccount() {
        UUID adminId = UUID.randomUUID();

        assertThrows(BadRequestException.class, () -> userService.deleteUser(adminId, adminId));
    }
}
