package com.helpdesk.ticketservice.repository;

import com.helpdesk.ticketservice.entity.Notification;
import com.helpdesk.ticketservice.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Used to de-duplicate SLA_WARNING notifications: since there is no background
     * worker, warnings are generated lazily whenever a ticket is viewed by staff.
     * This check prevents re-notifying about the same ticket on every page view.
     */
    boolean existsByUserIdAndTypeAndMessageContainingAndReadFalse(UUID userId, NotificationType type, String fragment);
}
