package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.model.ScenarioType;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.model.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlpiTicketMapperTest {
    @Test
    @DisplayName("GIVEN a complete GLPI ticket WHEN toTicket is called THEN all fields are copied correctly")
    void toTicket_copiesAllFields_whenGlpiTicketIsComplete() {
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1001");
        glpiTicket.put("name", "Server Enterprise-01 Maintenance");
        glpiTicket.put("content", "Planned patching outside business hours.");
        glpiTicket.put("status", 2);
        glpiTicket.put("date", "2026-09-22 09:15:00");

        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        assertNull(result.getId());
        assertEquals("1001", result.getGlpiTicketId());
        assertEquals("Server Enterprise-01 Maintenance", result.getTitle());
        assertEquals("Planned patching outside business hours.", result.getDescription());
        assertEquals(TicketStatus.IN_PROGRESS, result.getStatus());
        assertEquals(ScenarioType.SERVER_MAINTENANCE, result.getScenarioType());
        assertEquals(LocalDateTime.of(2026, 9, 22, 9,15, 0), result.getCreatedAt());
    }

    @Test
    @DisplayName("GIVEN a GLPI ticket with an unknown status WHEN toTicket is called THEN NEW is used as fallback")
    void toTicket_usesNewAsFallback_whenStatusIsUnknown() {
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1002");
        glpiTicket.put("name", "Unknown ticket");
        glpiTicket.put("status", 99);

        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        assertEquals(TicketStatus.NEW, result.getStatus());
    }

    @Test
    @DisplayName("GIVEN a GLPI ticket without a status field WHEN toTicket is called THEN NEW is used as fallback")
    void toTicket_usesNewAsFallback_whenStatusFieldIsMissing() {
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1003");
        glpiTicket.put("name", "Ticket without status");

        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        assertEquals(TicketStatus.NEW, result.getStatus());
    }

    @Test
    @DisplayName("GIVEN a GLPI ticket with an unreadable date WHEN toTicket is called THEN the current date is used as fallback")
    void toTicket_usesCurrentDateAsFallback_whenDateIsUnreadable() {
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1004");
        glpiTicket.put("name", "Ticket with broken date");
        glpiTicket.put("date", "not-a-valid-date");

        LocalDateTime beforeCall = LocalDateTime.now();

        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        LocalDateTime afterCall = LocalDateTime.now();

        assertTrue(!result.getCreatedAt().isBefore(beforeCall)
                && !result.getCreatedAt().isAfter(afterCall));
    }

    @Test
    @DisplayName("GIVEN a GLPI ticket without title and description WHEN toTicket is called THEN empty strings are used instead of null")
    void toTicket_usesEmptyStrings_whenTitleAndDescriptionAreMissing() {
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", "1005");

        Ticket result = GlpiTicketMapper.toTicket(glpiTicket);

        assertEquals("", result.getTitle());
        assertEquals("", result.getDescription());
    }
}
