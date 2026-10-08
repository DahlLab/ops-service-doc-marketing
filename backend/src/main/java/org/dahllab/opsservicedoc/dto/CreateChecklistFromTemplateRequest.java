package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateChecklistFromTemplateRequest(
        @NotBlank(message = "ticketId must not be empty")
        String ticketId,
        @NotBlank(message = "templateId must not be empty")
        String templateId
) {
}
