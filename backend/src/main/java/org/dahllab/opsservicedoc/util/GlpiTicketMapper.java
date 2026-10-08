package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.model.ScenarioType;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.model.TicketStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class GlpiTicketMapper {
    private static final DateTimeFormatter GLPI_DATE_FORMAT=
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String TECHNICIAN_NOT_ASSIGNED = "Nicht zugewiesen";

    private GlpiTicketMapper() {
    }

    public static Ticket toTicket(Map<String, Object> glpiTicket) {
        return new Ticket(
                null,
                extractGlpiId(glpiTicket),
                extractTitle(glpiTicket),
                extractDescription(glpiTicket),
                extractStatus(glpiTicket),
                TECHNICIAN_NOT_ASSIGNED,

                ScenarioType.SERVER_MAINTENANCE,
                extractCreatedAt(glpiTicket)
        );
    }

    private static String extractGlpiId(Map<String, Object> glpiTicket) {
        Object id = glpiTicket.get("id");
        return id != null ? id.toString() : null;
    }

    private static String extractTitle(Map<String, Object> glpiTicket) {
        Object name = glpiTicket.get("name");
        return name != null ? name.toString() : "";
    }

    private static String extractDescription(Map<String, Object> glpiTicket) {
        Object content = glpiTicket.get("content");
        return content != null ? content.toString() : "";
    }

    private static TicketStatus extractStatus(Map<String, Object> glpiTicket) {
        Object statusValue = glpiTicket.get("status");
        if (statusValue == null) {
            return TicketStatus.NEW;
        }

        int status = ((Number) statusValue).intValue();

        return switch (status) {
            case 1 -> TicketStatus.NEW;
            case 2, 3 -> TicketStatus.IN_PROGRESS;
            case 4 -> TicketStatus.PENDING;
            case 5 -> TicketStatus.SOLVED;
            case 6 -> TicketStatus.CLOSED;

            default -> TicketStatus.NEW;
        };
    }

    private static LocalDateTime extractCreatedAt(Map<String, Object> glpiTicket) {
        Object date = glpiTicket.get("date");
        if (date == null) {
            return LocalDateTime.now(ZoneId.systemDefault());
        }

        try {
            return LocalDateTime.parse(date.toString(), GLPI_DATE_FORMAT);
        } catch (Exception _) {
            return LocalDateTime.now(ZoneId.systemDefault());
        }
    }
}
