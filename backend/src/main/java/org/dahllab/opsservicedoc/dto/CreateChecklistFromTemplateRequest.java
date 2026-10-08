package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateChecklistFromTemplateRequest(
        @NotBlank(message = "ticketId darf nicht leer sein")
        String ticketId,
        @NotBlank(message = "templateId darf nicht leer sein")
        String templateId
) {
}
