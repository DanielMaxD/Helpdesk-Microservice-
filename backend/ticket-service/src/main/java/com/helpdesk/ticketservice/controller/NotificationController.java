package com.helpdesk.ticketservice.controller;

import com.helpdesk.ticketservice.dto.NotificationResponse;
import com.helpdesk.ticketservice.security.AuthUtils;
import com.helpdesk.ticketservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getUserNotifications(AuthUtils.getUserId(authentication)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAsRead(id, AuthUtils.getUserId(authentication)));
    }
}
