package org.dahllab.opsservicedoc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "tickets")

@Data

@NoArgsConstructor

@AllArgsConstructor
public class Ticket {
    @Id
    private String id;

    private String glpiTicketId;

    private String title;

    private String description;

    private TicketStatus status;

    private String technician;

    private ScenarioType scenarioType;

    private LocalDateTime createdAt;
}
