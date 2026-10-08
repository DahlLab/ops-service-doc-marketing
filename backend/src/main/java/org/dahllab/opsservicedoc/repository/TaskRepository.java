package org.dahllab.opsservicedoc.repository;

import org.dahllab.opsservicedoc.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

// MongoRepository liefert mir automatisch CRUD-Methoden (save, findById,
// findAll, deleteById, existsById, ...). ZUsätzlich brauche ich eine
// abgeleitete Query-Methode, um alle Tasks zu einem bestimmten Ticket zu
// finden, Spring Data generiert die Implementierung allein anhand des Methodennamens.
public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByTicketId(String ticketId);
}
