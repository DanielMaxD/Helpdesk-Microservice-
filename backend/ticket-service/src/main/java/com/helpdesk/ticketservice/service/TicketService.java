package com.helpdesk.ticketservice.service;

import com.helpdesk.ticketservice.client.UserServiceClient;
import com.helpdesk.ticketservice.dto.AssignRequest;
import com.helpdesk.ticketservice.dto.CommentRequest;
import com.helpdesk.ticketservice.dto.CommentResponse;
import com.helpdesk.ticketservice.dto.StatusUpdateRequest;
import com.helpdesk.ticketservice.dto.TicketCreateRequest;
import com.helpdesk.ticketservice.dto.TicketResponse;
import com.helpdesk.ticketservice.dto.TicketStatisticsResponse;
import com.helpdesk.ticketservice.dto.TicketUpdateRequest;
import com.helpdesk.ticketservice.dto.UserDto;
import com.helpdesk.ticketservice.entity.NotificationType;
import com.helpdesk.ticketservice.entity.Status;
import com.helpdesk.ticketservice.entity.Ticket;
import com.helpdesk.ticketservice.entity.TicketComment;
import com.helpdesk.ticketservice.dto.SlaState;
import com.helpdesk.ticketservice.exception.BadRequestException;
import com.helpdesk.ticketservice.exception.ResourceNotFoundException;
import com.helpdesk.ticketservice.repository.TicketCommentRepository;
import com.helpdesk.ticketservice.repository.TicketRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketService {

    /**
     * Valid lifecycle: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED.
     * Any target not present in the current status's set is rejected, including
     * skipping states (OPEN -> RESOLVED), going backwards, and any transition
     * out of CLOSED (a closed ticket is immutable).
     */
    private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS = new EnumMap<>(Status.class);

    static {
        ALLOWED_TRANSITIONS.put(Status.OPEN, EnumSet.of(Status.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(Status.IN_PROGRESS, EnumSet.of(Status.RESOLVED));
        ALLOWED_TRANSITIONS.put(Status.RESOLVED, EnumSet.of(Status.CLOSED));
        ALLOWED_TRANSITIONS.put(Status.CLOSED, EnumSet.noneOf(Status.class));
    }

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final NotificationService notificationService;
    private final UserServiceClient userServiceClient;
    private final SlaCalculator slaCalculator;

    public TicketService(
            TicketRepository ticketRepository,
            TicketCommentRepository ticketCommentRepository,
            NotificationService notificationService,
            UserServiceClient userServiceClient,
            SlaCalculator slaCalculator
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.notificationService = notificationService;
        this.userServiceClient = userServiceClient;
        this.slaCalculator = slaCalculator;
    }

    @Transactional
    public TicketResponse createTicket(TicketCreateRequest request, UUID requesterId) {
        Instant now = Instant.now();

        Ticket ticket = Ticket.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .priority(request.getPriority())
                .category(request.getCategory())
                .status(Status.OPEN)
                .createdBy(requesterId)
                .assignedAgent(null)
                .dueAt(slaCalculator.calculateDueAt(now, request.getPriority()))
                .build();

        // saveAndFlush (not save): toResponse() below immediately reads
        // ticket.getCreatedAt() via slaCalculator.calculateState(), and
        // createdAt is a @CreationTimestamp field that Hibernate only
        // guarantees is resolved on the managed entity once the insert has
        // actually been executed. A plain save() can defer that insert to
        // the transaction's eventual flush, leaving createdAt null on this
        // in-memory instance in the meantime - which would NPE inside
        // calculateState() on every ticket creation. Flushing immediately
        // here removes that ambiguity; it's a single-row insert, so there's
        // no batching benefit being given up.
        return toResponse(ticketRepository.saveAndFlush(ticket));
    }

    public TicketResponse getTicketById(UUID ticketId, UUID requesterId, String role) {
        Ticket ticket = findTicketOrThrow(ticketId);
        assertCanView(ticket, requesterId, role);

        TicketResponse response = toResponse(ticket);

        if (response.getSlaState() == SlaState.AT_RISK && ticket.getAssignedAgent() != null) {
            notificationService.createSlaWarningIfAbsent(ticket.getAssignedAgent(), ticket.getId(), ticket.getTitle());
        }

        return response;
    }

    public List<TicketResponse> getMyTickets(UUID requesterId) {
        return ticketRepository.findByCreatedBy(requesterId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TicketResponse> getAssignedTickets(UUID agentId) {
        return ticketRepository.findByAssignedAgent(agentId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TicketResponse updateTicket(UUID ticketId, TicketUpdateRequest request, UUID requesterId, String role) {
        Ticket ticket = findTicketOrThrow(ticketId);

        if (ticket.getStatus() == Status.CLOSED) {
            throw new BadRequestException("A closed ticket cannot be modified");
        }

        boolean isAdmin = "ADMIN".equals(role);
        boolean isAssignedAgent = "AGENT".equals(role)
                && ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().equals(requesterId);

        if (!isAdmin && !isAssignedAgent) {
            throw new AccessDeniedException("You do not have permission to modify this ticket");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            ticket.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            ticket.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null) {
            ticket.setCategory(request.getCategory());
        }
        if (request.getPriority() != null && request.getPriority() != ticket.getPriority()) {
            ticket.setPriority(request.getPriority());
            ticket.setDueAt(slaCalculator.calculateDueAt(ticket.getCreatedAt(), request.getPriority()));
        }

        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateStatus(UUID ticketId, StatusUpdateRequest request, UUID requesterId, String role) {
        Ticket ticket = findTicketOrThrow(ticketId);

        if (ticket.getStatus() == Status.CLOSED) {
            throw new BadRequestException("A closed ticket cannot be modified");
        }

        Status currentStatus = ticket.getStatus();
        Status targetStatus = request.getStatus();

        Set<Status> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedNext.contains(targetStatus)) {
            throw new BadRequestException(
                    "Cannot transition ticket from " + currentStatus + " to " + targetStatus);
        }

        switch (role) {
            case "ADMIN" -> {
                // Admins may perform any valid transition on any ticket.
            }
            case "AGENT" -> {
                if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().equals(requesterId)) {
                    throw new AccessDeniedException("You can only update tickets assigned to you");
                }
            }
            case "USER" -> {
                boolean isOwner = ticket.getCreatedBy().equals(requesterId);
                boolean isCloseAction = currentStatus == Status.RESOLVED && targetStatus == Status.CLOSED;
                if (!isOwner || !isCloseAction) {
                    throw new AccessDeniedException("You can only close your own resolved tickets");
                }
            }
            default -> throw new AccessDeniedException("Unknown role");
        }

        ticket.setStatus(targetStatus);
        if (targetStatus == Status.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        }

        Ticket saved = ticketRepository.save(ticket);

        if (!requesterId.equals(saved.getCreatedBy())) {
            NotificationType type = targetStatus == Status.RESOLVED
                    ? NotificationType.TICKET_RESOLVED
                    : NotificationType.TICKET_UPDATED;
            String message = targetStatus == Status.RESOLVED
                    ? "Your ticket \"" + saved.getTitle() + "\" has been resolved."
                    : "Your ticket \"" + saved.getTitle() + "\" status changed to " + targetStatus + ".";
            notificationService.createNotification(saved.getCreatedBy(), type, message);
        }

        return toResponse(saved);
    }

    @Transactional
    public TicketResponse assignTicket(UUID ticketId, AssignRequest request, String authorizationHeader) {
        Ticket ticket = findTicketOrThrow(ticketId);

        if (ticket.getStatus() == Status.CLOSED) {
            throw new BadRequestException("A closed ticket cannot be modified");
        }

        UserDto agent = userServiceClient.getUserById(request.getAgentId(), authorizationHeader);

        if (!"AGENT".equals(agent.getRole())) {
            throw new BadRequestException("Selected user is not an agent");
        }
        if (!agent.isActive()) {
            throw new BadRequestException("Selected agent is not active");
        }

        ticket.setAssignedAgent(agent.getId());
        Ticket saved = ticketRepository.save(ticket);

        notificationService.createNotification(
                agent.getId(),
                NotificationType.TICKET_ASSIGNED,
                "You have been assigned to ticket \"" + saved.getTitle() + "\"."
        );

        return toResponse(saved);
    }

    @Transactional
    public void deleteTicket(UUID ticketId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        ticketCommentRepository.deleteByTicketId(ticket.getId());
        ticketRepository.delete(ticket);
    }

    @Transactional
    public CommentResponse addComment(UUID ticketId, CommentRequest request, UUID requesterId, String role) {
        Ticket ticket = findTicketOrThrow(ticketId);

        if (ticket.getStatus() == Status.CLOSED) {
            throw new BadRequestException("Cannot comment on a closed ticket");
        }

        assertCanView(ticket, requesterId, role);

        TicketComment comment = TicketComment.builder()
                .ticketId(ticket.getId())
                .userId(requesterId)
                .message(request.getMessage().trim())
                .build();

        return toCommentResponse(ticketCommentRepository.save(comment));
    }

    public List<CommentResponse> getComments(UUID ticketId, UUID requesterId, String role) {
        Ticket ticket = findTicketOrThrow(ticketId);
        assertCanView(ticket, requesterId, role);

        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    public TicketStatisticsResponse getStatistics(UUID requesterId, String role) {
        List<Ticket> scoped = switch (role) {
            case "ADMIN" -> ticketRepository.findAll();
            case "AGENT" -> ticketRepository.findByAssignedAgent(requesterId);
            case "USER" -> ticketRepository.findByCreatedBy(requesterId);
            default -> List.of();
        };

        Instant now = Instant.now();

        long open = scoped.stream().filter(t -> t.getStatus() == Status.OPEN).count();
        long inProgress = scoped.stream().filter(t -> t.getStatus() == Status.IN_PROGRESS).count();
        long resolved = scoped.stream().filter(t -> t.getStatus() == Status.RESOLVED).count();
        long closed = scoped.stream().filter(t -> t.getStatus() == Status.CLOSED).count();
        long breached = scoped.stream()
                .filter(t -> t.getStatus() == Status.OPEN || t.getStatus() == Status.IN_PROGRESS)
                .filter(t -> slaCalculator.calculateState(t, now) == SlaState.BREACHED)
                .count();

        return TicketStatisticsResponse.builder()
                .totalTickets(scoped.size())
                .openTickets(open)
                .inProgressTickets(inProgress)
                .resolvedTickets(resolved)
                .closedTickets(closed)
                .breachedTickets(breached)
                .build();
    }

    private void assertCanView(Ticket ticket, UUID requesterId, String role) {
        boolean allowed = switch (role) {
            case "ADMIN" -> true;
            case "AGENT" -> ticket.getAssignedAgent() != null && ticket.getAssignedAgent().equals(requesterId);
            case "USER" -> ticket.getCreatedBy().equals(requesterId);
            default -> false;
        };
        if (!allowed) {
            throw new AccessDeniedException("You do not have permission to view this ticket");
        }
    }

    private Ticket findTicketOrThrow(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    private TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .category(ticket.getCategory())
                .createdBy(ticket.getCreatedBy())
                .assignedAgent(ticket.getAssignedAgent())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .dueAt(ticket.getDueAt())
                .slaState(slaCalculator.calculateState(ticket, Instant.now()))
                .build();
    }

    private CommentResponse toCommentResponse(TicketComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicketId())
                .userId(comment.getUserId())
                .message(comment.getMessage())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
