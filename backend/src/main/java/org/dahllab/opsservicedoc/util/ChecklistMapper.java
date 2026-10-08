package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.dto.ChecklistDto;
import org.dahllab.opsservicedoc.dto.ChecklistItemDto;
import org.dahllab.opsservicedoc.model.Checklist;
import org.dahllab.opsservicedoc.model.ChecklistItem;

import java.util.List;

// Wandelt Checklist (Mongo-Dokument) in ChecklistDto (API-Ebene) um.
// Ich mappe hier nur in eine Richtung, genau wie bei TaskMapper - die
// umgekehrte Richtung baut der Service selbst zusammen, weil dort noch
// Zusatzlogik reinspielt (z.B. IDs für neue Items vergeben).
public class ChecklistMapper {

    private ChecklistMapper() {
        // Utility-Klasse, keine Instanzen nötig.
    }

    public static ChecklistDto toDto(Checklist checklist) {
        return new ChecklistDto(
                checklist.getId(),
                checklist.getTicketId(),
                checklist.getTitle(),
                toItemDtoList(checklist.getItems()),
                checklist.getCreatedAt(),
                checklist.getCompletedAt()
        );
    }

    private static List<ChecklistItemDto> toItemDtoList(List<ChecklistItem> items) {
        return items.stream()
                .map(item -> new ChecklistItemDto(item.getId(), item.getDescription(), item.isDone()))
                .toList();
    }
}