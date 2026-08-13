package com.helpdesk.ticketservice.repository;

import com.helpdesk.ticketservice.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByCreatedBy(UUID createdBy);

    List<Ticket> findByAssignedAgent(UUID assignedAgent);
}
