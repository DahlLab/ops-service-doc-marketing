package org.dahllab.opsservicedoc.repository;

import org.dahllab.opsservicedoc.model.ChecklistTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChecklistTemplateRepository extends MongoRepository<ChecklistTemplate, String> {
}