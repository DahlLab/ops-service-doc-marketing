package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.dto.TicketDto;
import org.dahllab.opsservicedoc.model.Ticket;

// Mapper-Klasse: wandelt zwischen Ticket (Model/Datenbank) und TicketDto
// (API-Antwort) um. Hält diese Umwandlungs-Logik an EINER zentralen Stelle,
// statt sie in jedem Controller/Service einzeln zu wiederholen (DRY).
public class TicketMapper {

    // Privater Konstruktor: diese Klasse enthält nur statische Methoden
    // und soll nicht instanziiert werden (reine Utility-Klasse).
    private TicketMapper() {

    }

    // Wandelt ein Ticket (aus der Datenbank) in ein TicketDto (für die API)
    public static TicketDto toDto(Ticket ticket) {
        return new TicketDto(
           ticket.getId(),
           ticket.getTitel(),
           ticket.getBeschreibung(),
           ticket.getStatus(),
           ticket.getTechniker(),
           ticket.getSzenarioTyp(),
           ticket.getErstelltAm()
        );
    }
}
