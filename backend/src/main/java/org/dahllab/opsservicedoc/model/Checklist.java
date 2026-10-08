package org.dahllab.opsservicedoc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection ="checklists")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Checklist {
    private String id;
    private String ticketId;
    private String title;
    private List<ChecklistItem> items;
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
