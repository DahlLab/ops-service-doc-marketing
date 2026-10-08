package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.dto.ChecklistDto;
import org.dahllab.opsservicedoc.dto.ChecklistItemDto;
import org.dahllab.opsservicedoc.model.Checklist;
import org.dahllab.opsservicedoc.model.ChecklistItem;

import java.util.List;

public class ChecklistMapper {
    private ChecklistMapper() {
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
