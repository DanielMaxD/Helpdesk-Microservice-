package com.helpdesk.ticketservice.service;

import com.helpdesk.ticketservice.dto.NotificationResponse;
import com.helpdesk.ticketservice.entity.Notification;
import com.helpdesk.ticketservice.entity.NotificationType;
import com.helpdesk.ticketservice.exception.ResourceNotFoundException;
import com.helpdesk.ticketservice.repository.NotificationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void createNotification(UUID userId, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    /**
     * Called from TicketService whenever a ticket is viewed and found to be
     * AT_RISK. De-duplicated so refreshing the same ticket repeatedly doesn't
     * spam the assigned agent with identical warnings.
     */
    @Transactional
    public void createSlaWarningIfAbsent(UUID agentId, UUID ticketId, String ticketTitle) {
        String fragment = "Ticket #" + ticketId;
        boolean alreadyWarned = notificationRepository
                .existsByUserIdAndTypeAndMessageContainingAndReadFalse(agentId, NotificationType.SLA_WARNING, fragment);

        if (!alreadyWarned) {
            createNotification(
                    agentId,
                    NotificationType.SLA_WARNING,
                    fragment + " (\"" + ticketTitle + "\") is at risk of breaching its SLA deadline."
            );
        }
    }

    public List<NotificationResponse> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID requesterId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUserId().equals(requesterId)) {
            throw new AccessDeniedException("You cannot modify another user's notification");
        }

        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
