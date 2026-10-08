package org.dahllab.opsservicedoc.dto;

import jakarta.validation.constraints.NotBlank;
import org.dahllab.opsservicedoc.model.IpdDocumentStatus;
import org.dahllab.opsservicedoc.model.ScenarioType;

import java.time.LocalDateTime;

public record IpdDocumentDto(
        String id,
        String ticketId,
        IpdDocumentStatus status,
        @NotBlank(message = "Titel darf nicht leer sein")
        String title,
        String technician,
        ScenarioType scenarioType,
        String customer,
        String customerContact,
        String period,
        String initialSituation,
        String requirements,
        String infrastructureOverview,
        String serversAndVms,
        String network,
        String rolesAndResponsibilities,
        String backupPlan,
        String securityConsiderations,
        String performedSteps,
        String decisions,
        String risksAndAssumptions,
        String rollbackPlan,
        boolean qualityAssuranceCompleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
