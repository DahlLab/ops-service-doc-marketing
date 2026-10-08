package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.dto.IpdDocumentDto;
import org.dahllab.opsservicedoc.model.IpdDocument;

public class IpdDocumentMapper {
    private IpdDocumentMapper() {
    }

    public static IpdDocumentDto toDto(IpdDocument ipdDocument) {
        return new IpdDocumentDto(
                ipdDocument.getId(),
                ipdDocument.getTicketId(),
                ipdDocument.getStatus(),
                ipdDocument.getTitle(),
                ipdDocument.getTechnician(),
                ipdDocument.getScenarioType(),
                ipdDocument.getCustomer(),
                ipdDocument.getCustomerContact(),
                ipdDocument.getPeriod(),
                ipdDocument.getInitialSituation(),
                ipdDocument.getRequirements(),
                ipdDocument.getInfrastructureOverview(),
                ipdDocument.getServersAndVms(),
                ipdDocument.getNetwork(),
                ipdDocument.getRolesAndResponsibilities(),
                ipdDocument.getBackupPlan(),
                ipdDocument.getSecurityConsiderations(),
                ipdDocument.getPerformedSteps(),
                ipdDocument.getDecisions(),
                ipdDocument.getRisksAndAssumptions(),
                ipdDocument.getRollbackPlan(),
                ipdDocument.isQualityAssuranceCompleted(),
                ipdDocument.getCreatedAt(),
                ipdDocument.getUpdatedAt()
        );
    }
}
