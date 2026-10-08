package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;

// Request-Body für POST /api/checklists/from-template: ich brauche nur
// die Referenz zum Ticket und zur gewünschten Vorlage, die Items
// erzeugt der Service daraus selbst.
public record CreateChecklistFromTemplateRequest(
        @NotBlank(message = "ticketId darf nicht leer sein")
        String ticketId,
        @NotBlank(message = "templateId darf nicht leer sein")
        String templateId
) {
}