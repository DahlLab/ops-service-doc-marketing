package org.dahllab.opsservicedoc.dto;

import org.dahllab.opsservicedoc.model.ScenarioType;
import org.dahllab.opsservicedoc.model.TicketStatus;

import java.time.LocalDateTime;

public record TicketDto(
    String id,
    String title,
    String description,
    TicketStatus status,
    String technician,
    ScenarioType scenarioType,
    LocalDateTime createdAt
) {
}

