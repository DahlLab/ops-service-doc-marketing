package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.dto.TicketDto;
import org.dahllab.opsservicedoc.model.Ticket;

public class TicketMapper {
    private TicketMapper() {
    }

    public static TicketDto toDto(Ticket ticket) {
        return new TicketDto(
           ticket.getId(),
           ticket.getTitle(),
           ticket.getDescription(),
           ticket.getStatus(),
           ticket.getTechnician(),
           ticket.getScenarioType(),
           ticket.getCreatedAt()
        );
    }
}
