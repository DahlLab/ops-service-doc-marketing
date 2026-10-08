package org.dahllab.opsservicedoc.repository;

import org.dahllab.opsservicedoc.model.IpdDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IpdDocumentRepository extends MongoRepository<IpdDocument, String> {
    List<IpdDocument> findByTicketId(String ticketId);
}
