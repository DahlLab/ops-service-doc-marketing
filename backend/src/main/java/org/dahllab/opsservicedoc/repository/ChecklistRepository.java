package org.dahllab.opsservicedoc.repository;

import org.dahllab.opsservicedoc.model.Checklist;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChecklistRepository extends MongoRepository<Checklist, String> {
    List<Checklist> findByTicketId(String ticketId);
}