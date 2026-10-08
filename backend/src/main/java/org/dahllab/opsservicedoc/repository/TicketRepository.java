package org.dahllab.opsservicedoc.repository;

import org.dahllab.opsservicedoc.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TicketRepository extends MongoRepository<Ticket, String> {
    Optional<Ticket> findByGlpiTicketId(String glpiTicketId);
}
