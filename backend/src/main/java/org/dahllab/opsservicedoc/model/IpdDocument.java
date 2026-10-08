package org.dahllab.opsservicedoc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "ipd_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IpdDocument {
    @Id
    private String id;

    private String ticketId;

    private IpdDocumentStatus status;

    private String title;
    private String technician;
    private ScenarioType scenarioType;

    private String customer;
    private String customerContact;
    private String period;

    private String initialSituation;
    private String requirements;
    private String infrastructureOverview;
    private String serversAndVms;
    private String network;
    private String rolesAndResponsibilities;
    private String backupPlan;
    private String securityConsiderations;

    private String performedSteps;

    private String decisions;
    private String risksAndAssumptions;
    private String rollbackPlan;

    private boolean qualityAssuranceCompleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
