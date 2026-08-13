package com.helpdesk.ticketservice.controller;

import com.helpdesk.ticketservice.dto.AssignRequest;
import com.helpdesk.ticketservice.dto.CommentRequest;
import com.helpdesk.ticketservice.dto.CommentResponse;
import com.helpdesk.ticketservice.dto.StatusUpdateRequest;
import com.helpdesk.ticketservice.dto.TicketCreateRequest;
import com.helpdesk.ticketservice.dto.TicketResponse;
import com.helpdesk.ticketservice.dto.TicketStatisticsResponse;
import com.helpdesk.ticketservice.dto.TicketUpdateRequest;
import com.helpdesk.ticketservice.security.AuthUtils;
import com.helpdesk.ticketservice.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketCreateRequest request,
            Authentication authentication
    ) {
        TicketResponse response = ticketService.createTicket(request, AuthUtils.getUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable UUID id, Authentication authentication) {
        TicketResponse response = ticketService.getTicketById(
                id, AuthUtils.getUserId(authentication), AuthUtils.getRole(authentication));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<TicketResponse>> getMyTickets(Authentication authentication) {
        return ResponseEntity.ok(ticketService.getMyTickets(AuthUtils.getUserId(authentication)));
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<TicketResponse>> getAssignedTickets(Authentication authentication) {
        return ResponseEntity.ok(ticketService.getAssignedTickets(AuthUtils.getUserId(authentication)));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody TicketUpdateRequest request,
            Authentication authentication
    ) {
        TicketResponse response = ticketService.updateTicket(
                id, request, AuthUtils.getUserId(authentication), AuthUtils.getRole(authentication));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication
    ) {
        TicketResponse response = ticketService.updateStatus(
                id, request, AuthUtils.getUserId(authentication), AuthUtils.getRole(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN only (enforced in SecurityConfig). The caller's own bearer token is
     * forwarded to user-service so it can apply its own authorization rules
     * when verifying the target user exists, is an AGENT, and is active.
     */
    @PutMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return ResponseEntity.ok(ticketService.assignTicket(id, request, authorizationHeader));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable UUID id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        CommentResponse response = ticketService.addComment(
                id, request, AuthUtils.getUserId(authentication), AuthUtils.getRole(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID id, Authentication authentication) {
        List<CommentResponse> response = ticketService.getComments(
                id, AuthUtils.getUserId(authentication), AuthUtils.getRole(authentication));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<TicketStatisticsResponse> getStatistics(Authentication authentication) {
        TicketStatisticsResponse response = ticketService.getStatistics(
                AuthUtils.getUserId(authentication), AuthUtils.getRole(authentication));
        return ResponseEntity.ok(response);
    }
}
