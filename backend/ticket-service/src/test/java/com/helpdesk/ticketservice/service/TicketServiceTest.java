package com.helpdesk.ticketservice.service;

import com.helpdesk.ticketservice.client.UserServiceClient;
import com.helpdesk.ticketservice.dto.AssignRequest;
import com.helpdesk.ticketservice.dto.StatusUpdateRequest;
import com.helpdesk.ticketservice.dto.TicketCreateRequest;
import com.helpdesk.ticketservice.dto.TicketResponse;
import com.helpdesk.ticketservice.dto.UserDto;
import com.helpdesk.ticketservice.entity.Category;
import com.helpdesk.ticketservice.entity.Priority;
import com.helpdesk.ticketservice.entity.Status;
import com.helpdesk.ticketservice.entity.Ticket;
import com.helpdesk.ticketservice.exception.BadRequestException;
import com.helpdesk.ticketservice.repository.TicketCommentRepository;
import com.helpdesk.ticketservice.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserServiceClient userServiceClient;

    private TicketService ticketService;

    private UUID userId;
    private UUID otherUserId;
    private UUID agentId;
    private UUID otherAgentId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository, ticketCommentRepository, notificationService, userServiceClient, new SlaCalculator());

        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        otherAgentId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        lenient().when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------- creation ----------

    @Test
    void createTicketSetsOpenStatusCreatorAndDueDate() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("Cannot access my account");
        request.setDescription("I get an error every time I try to log in.");
        request.setPriority(Priority.HIGH);
        request.setCategory(Category.ACCOUNT);

        withGeneratedId();

        TicketResponse response = ticketService.createTicket(request, userId);

        assertEquals(Status.OPEN, response.getStatus());
        assertEquals(userId, response.getCreatedBy());
        assertEquals(null, response.getAssignedAgent());
    }

    // ---------- ownership / view access ----------

    @Test
    void userCanViewOwnTicket() {
        Ticket ticket = ticket(Status.OPEN, userId, null);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.getTicketById(ticket.getId(), userId, "USER");

        assertEquals(ticket.getId(), response.getId());
    }

    @Test
    void userCannotViewAnotherUsersTicket() {
        Ticket ticket = ticket(Status.OPEN, otherUserId, null);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(AccessDeniedException.class,
                () -> ticketService.getTicketById(ticket.getId(), userId, "USER"));
    }

    @Test
    void agentCanViewAssignedTicket() {
        Ticket ticket = ticket(Status.IN_PROGRESS, userId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.getTicketById(ticket.getId(), agentId, "AGENT");

        assertEquals(ticket.getId(), response.getId());
    }

    @Test
    void agentCannotViewUnassignedTicket() {
        Ticket ticket = ticket(Status.OPEN, userId, null);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(AccessDeniedException.class,
                () -> ticketService.getTicketById(ticket.getId(), agentId, "AGENT"));
    }

    @Test
    void agentCannotViewTicketAssignedToAnotherAgent() {
        Ticket ticket = ticket(Status.IN_PROGRESS, userId, otherAgentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(AccessDeniedException.class,
                () -> ticketService.getTicketById(ticket.getId(), agentId, "AGENT"));
    }

    @Test
    void adminCanViewAnyTicket() {
        Ticket ticket = ticket(Status.OPEN, userId, null);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.getTicketById(ticket.getId(), adminId, "ADMIN");

        assertEquals(ticket.getId(), response.getId());
    }

    // ---------- status transitions ----------

    @Test
    void rejectsSkippingOpenToResolved() {
        Ticket ticket = ticket(Status.OPEN, userId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.RESOLVED);

        assertThrows(BadRequestException.class,
                () -> ticketService.updateStatus(ticket.getId(), request, agentId, "AGENT"));
    }

    @Test
    void rejectsReopeningClosedTicket() {
        Ticket ticket = ticket(Status.CLOSED, userId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.OPEN);

        assertThrows(BadRequestException.class,
                () -> ticketService.updateStatus(ticket.getId(), request, adminId, "ADMIN"));
    }

    @Test
    void agentCanProgressAssignedTicketFromOpenToInProgress() {
        Ticket ticket = ticket(Status.OPEN, userId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.IN_PROGRESS);

        TicketResponse response = ticketService.updateStatus(ticket.getId(), request, agentId, "AGENT");

        assertEquals(Status.IN_PROGRESS, response.getStatus());
    }

    @Test
    void agentCannotChangeStatusOfUnassignedTicket() {
        Ticket ticket = ticket(Status.OPEN, userId, otherAgentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.IN_PROGRESS);

        assertThrows(AccessDeniedException.class,
                () -> ticketService.updateStatus(ticket.getId(), request, agentId, "AGENT"));
    }

    @Test
    void resolvingTicketSetsResolvedAtAndNotifiesCreator() {
        Ticket ticket = ticket(Status.IN_PROGRESS, userId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.RESOLVED);

        TicketResponse response = ticketService.updateStatus(ticket.getId(), request, agentId, "AGENT");

        assertEquals(Status.RESOLVED, response.getStatus());
        verify(notificationService, times(1))
                .createNotification(eq(userId), any(), anyString());
    }

    @Test
    void userCanCloseTheirOwnResolvedTicket() {
        Ticket ticket = ticket(Status.RESOLVED, userId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.CLOSED);

        TicketResponse response = ticketService.updateStatus(ticket.getId(), request, userId, "USER");

        assertEquals(Status.CLOSED, response.getStatus());
    }

    @Test
    void userCannotCloseAnotherUsersResolvedTicket() {
        Ticket ticket = ticket(Status.RESOLVED, otherUserId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.CLOSED);

        assertThrows(AccessDeniedException.class,
                () -> ticketService.updateStatus(ticket.getId(), request, userId, "USER"));
    }

    @Test
    void userCannotMoveTicketFromOpenToInProgress() {
        Ticket ticket = ticket(Status.OPEN, userId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.IN_PROGRESS);

        assertThrows(AccessDeniedException.class,
                () -> ticketService.updateStatus(ticket.getId(), request, userId, "USER"));
    }

    // ---------- assignment ----------

    @Test
    void assignRejectsWhenSelectedUserIsNotAnAgent() {
        Ticket ticket = ticket(Status.OPEN, userId, null);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        UUID notAnAgentId = UUID.randomUUID();
        UserDto notAnAgent = new UserDto(notAnAgentId, "Not Agent", "notagent@helpdesk.dev", "USER", true, Instant.now());
        when(userServiceClient.getUserById(eq(notAnAgentId), anyString())).thenReturn(notAnAgent);

        AssignRequest request = new AssignRequest();
        request.setAgentId(notAnAgentId);

        assertThrows(BadRequestException.class,
                () -> ticketService.assignTicket(ticket.getId(), request, "Bearer admin-token"));
    }

    @Test
    void assignRejectsWhenAgentIsInactive() {
        Ticket ticket = ticket(Status.OPEN, userId, null);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        UserDto inactiveAgent = new UserDto(agentId, "Inactive Agent", "agent1@helpdesk.dev", "AGENT", false, Instant.now());
        when(userServiceClient.getUserById(eq(agentId), anyString())).thenReturn(inactiveAgent);

        AssignRequest request = new AssignRequest();
        request.setAgentId(agentId);

        assertThrows(BadRequestException.class,
                () -> ticketService.assignTicket(ticket.getId(), request, "Bearer admin-token"));
    }

    @Test
    void assignSucceedsForActiveAgentAndCreatesNotification() {
        Ticket ticket = ticket(Status.OPEN, userId, null);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        UserDto activeAgent = new UserDto(agentId, "Gina Agent", "agent1@helpdesk.dev", "AGENT", true, Instant.now());
        when(userServiceClient.getUserById(eq(agentId), anyString())).thenReturn(activeAgent);

        AssignRequest request = new AssignRequest();
        request.setAgentId(agentId);

        TicketResponse response = ticketService.assignTicket(ticket.getId(), request, "Bearer admin-token");

        assertEquals(agentId, response.getAssignedAgent());
        verify(notificationService, times(1))
                .createNotification(eq(agentId), any(), anyString());
    }

    @Test
    void cannotAssignAClosedTicket() {
        Ticket ticket = ticket(Status.CLOSED, userId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        AssignRequest request = new AssignRequest();
        request.setAgentId(agentId);

        assertThrows(BadRequestException.class,
                () -> ticketService.assignTicket(ticket.getId(), request, "Bearer admin-token"));
        verify(userServiceClient, never()).getUserById(any(), anyString());
    }

    // ---------- closed ticket immutability ----------

    @Test
    void closedTicketCannotBeGeneralUpdated() {
        Ticket ticket = ticket(Status.CLOSED, userId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        com.helpdesk.ticketservice.dto.TicketUpdateRequest request = new com.helpdesk.ticketservice.dto.TicketUpdateRequest();
        request.setTitle("Trying to sneak an edit past a closed ticket");

        assertThrows(BadRequestException.class,
                () -> ticketService.updateTicket(ticket.getId(), request, adminId, "ADMIN"));
    }

    @Test
    void cannotCommentOnAClosedTicket() {
        Ticket ticket = ticket(Status.CLOSED, userId, agentId);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        com.helpdesk.ticketservice.dto.CommentRequest request = new com.helpdesk.ticketservice.dto.CommentRequest();
        request.setMessage("One more thing...");

        assertThrows(BadRequestException.class,
                () -> ticketService.addComment(ticket.getId(), request, userId, "USER"));
    }

    // ---------- helpers ----------

    private void withGeneratedId() {
        // Stubs both save() and saveAndFlush() with the same behavior, since
        // this simulates what Hibernate itself does on insert (assigning the
        // generated id/createdAt/updatedAt) regardless of which repository
        // method the service calls. createTicket() specifically calls
        // saveAndFlush() (see TicketService for why), other paths still call
        // plain save() on already-persisted tickets.
        org.mockito.stubbing.Answer<Ticket> generateOnSave = invocation -> {
            Ticket t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            if (t.getCreatedAt() == null) {
                t.setCreatedAt(Instant.now());
            }
            if (t.getUpdatedAt() == null) {
                t.setUpdatedAt(Instant.now());
            }
            return t;
        };
        lenient().when(ticketRepository.save(any(Ticket.class))).thenAnswer(generateOnSave);
        lenient().when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(generateOnSave);
    }

    private Ticket ticket(Status status, UUID createdBy, UUID assignedAgent) {
        Instant now = Instant.now();
        return Ticket.builder()
                .id(UUID.randomUUID())
                .title("Sample ticket")
                .description("Sample description long enough to pass validation.")
                .priority(Priority.MEDIUM)
                .category(Category.TECHNICAL)
                .status(status)
                .createdBy(createdBy)
                .assignedAgent(assignedAgent)
                .createdAt(now.minus(1, ChronoUnit.HOURS))
                .updatedAt(now.minus(1, ChronoUnit.HOURS))
                .resolvedAt(status == Status.RESOLVED || status == Status.CLOSED ? now.minus(30, ChronoUnit.MINUTES) : null)
                .dueAt(now.plus(7, ChronoUnit.HOURS))
                .build();
    }
}
