package com.helpdesk.userservice.service;

import com.helpdesk.userservice.dto.AuthResponse;
import com.helpdesk.userservice.dto.LoginRequest;
import com.helpdesk.userservice.dto.RegisterRequest;
import com.helpdesk.userservice.entity.Role;
import com.helpdesk.userservice.entity.User;
import com.helpdesk.userservice.exception.BadRequestException;
import com.helpdesk.userservice.repository.UserRepository;
import com.helpdesk.userservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserWithUserRole() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Jane Doe");
        request.setEmail("Jane@Example.com");
        request.setPassword("securePass1");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePass1")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("fake-jwt-token", response.getToken());
        assertEquals(Role.USER, response.getUser().getRole());
        assertEquals("jane@example.com", response.getUser().getEmail());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("securePass1");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("securePass1");

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .name("Jane Doe")
                .email("jane@example.com")
                .passwordHash("hashed-password")
                .role(Role.USER)
                .active(true)
                .build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("securePass1", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(existingUser)).thenReturn("fake-jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("jane@example.com", response.getUser().getEmail());
    }

    @Test
    void loginFailsWithWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("wrongPassword");

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("jane@example.com")
                .passwordHash("hashed-password")
                .role(Role.USER)
                .active(true)
                .build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void loginFailsForDeactivatedUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("securePass1");

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("jane@example.com")
                .passwordHash("hashed-password")
                .role(Role.USER)
                .active(false)
                .build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("securePass1", "hashed-password")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }
}
