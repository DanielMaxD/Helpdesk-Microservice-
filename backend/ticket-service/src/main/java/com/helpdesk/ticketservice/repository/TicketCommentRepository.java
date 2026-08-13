package com.helpdesk.ticketservice.repository;

import com.helpdesk.ticketservice.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {

    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    void deleteByTicketId(UUID ticketId);
}
