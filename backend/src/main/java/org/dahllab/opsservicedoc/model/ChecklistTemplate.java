package org.dahllab.opsservicedoc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

// Eine wiederverwendbare Vorlage für eine Checkliste (z.B. "Server-
// Wartung Standard"). Ich speichere hier bewusst nur die reinen
// Beschreibungstexte der Punkte, ohne erledigt-Status - eine Vorlage
// selbst kann ja nicht "erledigt" sein, das entsteht erst, wenn daraus
// eine konkrete Checkliste für ein Ticket erzeugt wird.
@Document(collection = "checklist_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplate {
    private String id;
    private String name;
    private List<String> itemBeschreibungen;
    // true nur bei den zehn vom ChecklistTemplateSeeder beim Start
    // angelegten Standard-Vorlagen (Server, Netzwerk, ...). Eine
    // Standard-Vorlage darf nicht gelöscht werden (siehe
    // ChecklistTemplateService.deleteTemplate) - selbst angelegte
    // Vorlagen haben standard=false und bleiben jederzeit löschbar.
    private boolean standard;
}