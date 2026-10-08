package org.dahllab.opsservicedoc.repository;

import org.dahllab.opsservicedoc.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByTicketId(String ticketId);
}
