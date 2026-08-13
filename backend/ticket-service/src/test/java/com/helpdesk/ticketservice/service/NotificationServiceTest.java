package com.helpdesk.ticketservice.service;

import com.helpdesk.ticketservice.dto.NotificationResponse;
import com.helpdesk.ticketservice.entity.Notification;
import com.helpdesk.ticketservice.entity.NotificationType;
import com.helpdesk.ticketservice.exception.ResourceNotFoundException;
import com.helpdesk.ticketservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void markAsReadSucceedsForOwner() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type(NotificationType.TICKET_ASSIGNED)
                .message("You have been assigned to ticket \"Sample\".")
                .read(false)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(notificationId, userId);

        assertTrue(response.isRead());
    }

    @Test
    void markAsReadRejectsNonOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(ownerId)
                .type(NotificationType.TICKET_ASSIGNED)
                .message("You have been assigned to ticket \"Sample\".")
                .read(false)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class,
                () -> notificationService.markAsRead(notificationId, strangerId));
    }

    @Test
    void markAsReadThrowsWhenNotificationMissing() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(notificationId, UUID.randomUUID()));
    }

    @Test
    void slaWarningIsNotDuplicatedForSameTicket() {
        UUID agentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        when(notificationRepository.existsByUserIdAndTypeAndMessageContainingAndReadFalse(
                any(), any(), any())).thenReturn(true);

        notificationService.createSlaWarningIfAbsent(agentId, ticketId, "Some ticket");

        org.mockito.Mockito.verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }
}
