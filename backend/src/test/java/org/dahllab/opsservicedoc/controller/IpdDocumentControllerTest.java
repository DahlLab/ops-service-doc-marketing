package org.dahllab.opsservicedoc.controller;

import org.dahllab.opsservicedoc.model.Checklist;
import org.dahllab.opsservicedoc.model.ChecklistItem;
import org.dahllab.opsservicedoc.model.IpdDocument;
import org.dahllab.opsservicedoc.model.IpdDocumentStatus;
import org.dahllab.opsservicedoc.model.ScenarioType;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.model.TicketStatus;
import org.dahllab.opsservicedoc.repository.ChecklistRepository;
import org.dahllab.opsservicedoc.repository.IpdDocumentRepository;
import org.dahllab.opsservicedoc.repository.TaskRepository;
import org.dahllab.opsservicedoc.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IpdDocumentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IpdDocumentRepository ipdDocumentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ChecklistRepository checklistRepository;

    @BeforeEach
    void setUp() {
        ipdDocumentRepository.deleteAll();
        ticketRepository.deleteAll();
        taskRepository.deleteAll();
        checklistRepository.deleteAll();
    }

    @Test
    void getAllIpdDocuments_returnsEmptyList_whenNoDocumentsExist() throws Exception {
        mockMvc.perform(get("/api/ipd").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void postIpdDocumentFromTicket_createsDraft_andReturns201() throws Exception {
        Ticket savedTicket = ticketRepository.save(new Ticket(
                null, null, "Server-Wartung", "Beschreibung", TicketStatus.IN_PROGRESS,
                "Mia Muster", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now()));

        mockMvc.perform(post("/api/ipd/from-ticket/" + savedTicket.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Server-Wartung"))
                .andExpect(jsonPath("$.technician").value("Mia Muster"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void postIpdDocumentFromTicket_returns404_whenTicketDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/ipd/from-ticket/unbekannt").with(oauth2Login()).with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getIpdDocumentById_returns404_whenIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/ipd/unbekannt").with(oauth2Login()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void putIpdDocument_updatesFieldsAndStatus() throws Exception {
        Ticket savedTicket = ticketRepository.save(new Ticket(
                null, null, "Server-Wartung", "Beschreibung", TicketStatus.IN_PROGRESS,
                "Mia Muster", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now()));
        IpdDocument savedDocument = ipdDocumentRepository.save(new IpdDocument(
                null, savedTicket.getId(), IpdDocumentStatus.DRAFT, "Server-Wartung",
                "Mia Muster", ScenarioType.SERVER_MAINTENANCE, null, null, null, null, null, null, null,
                null, null, null, null, "", null, null, null, false,
                LocalDateTime.now(), LocalDateTime.now()));

        String requestBody = """
                {
                    "ticketId": "%s",
                    "status": "COMPLETED",
                    "title": "Server-Wartung",
                    "customer": "Musterfirma GmbH",
                    "customerContact": "Herr Beispiel",
                    "period": "14.09.2026 - 16.09.2026",
                    "initialSituation": "USV ausgefallen",
                    "qualityAssuranceCompleted": false
                }
                """.formatted(savedTicket.getId());

        mockMvc.perform(put("/api/ipd/" + savedDocument.getId())
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.customer").value("Musterfirma GmbH"));
    }

    @Test
    void deleteIpdDocument_removesDocument_andReturns204() throws Exception {
        IpdDocument savedDocument = ipdDocumentRepository.save(new IpdDocument(
                null, "ticket-1", IpdDocumentStatus.DRAFT, "Server-Wartung", "Mia Muster",
                ScenarioType.SERVER_MAINTENANCE, null, null, null, null, null, null, null, null, null,
                null, null, "", null, null, null, false, LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(delete("/api/ipd/" + savedDocument.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getPdf_returnsPdfFile() throws Exception {
        IpdDocument savedDocument = ipdDocumentRepository.save(new IpdDocument(
                null, "ticket-1", IpdDocumentStatus.DRAFT, "Server-Wartung", "Mia Muster",
                ScenarioType.SERVER_MAINTENANCE, "Musterfirma GmbH", null, null, "USV ausgefallen", null,
                null, null, null, null, null, null, "", null, null, null, false,
                LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(get("/api/ipd/" + savedDocument.getId() + "/pdf").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void getChecklistPdf_returnsPdfFile_whenChecklistExists() throws Exception {
        IpdDocument ipdDocument = ipdDocumentRepository.save(new IpdDocument(
                null, "ticket-1", IpdDocumentStatus.DRAFT, "Server-Wartung", "Mia Muster",
                ScenarioType.SERVER_MAINTENANCE, "Musterfirma GmbH", null, null, null, null,
                null, null, null, null, null, null, "", null, null, null, false,
                LocalDateTime.now(), LocalDateTime.now()));
        checklistRepository.save(new Checklist(null, "ticket-1", "Wartung",
                List.of(new ChecklistItem("i-1", "USV geprüft", false)), LocalDateTime.now(), null));

        mockMvc.perform(get("/api/ipd/" + ipdDocument.getId() + "/checklist-pdf").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"checkliste-" + ipdDocument.getId() + ".pdf\""));
    }

    @Test
    void getChecklistPdf_returns404_whenNoChecklistExists() throws Exception {
        IpdDocument ipdDocument = ipdDocumentRepository.save(new IpdDocument(
                null, "ticket-1", IpdDocumentStatus.DRAFT, "Server-Wartung", "Mia Muster",
                ScenarioType.SERVER_MAINTENANCE, "Musterfirma GmbH", null, null, null, null,
                null, null, null, null, null, null, "", null, null, null, false,
                LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(get("/api/ipd/" + ipdDocument.getId() + "/checklist-pdf").with(oauth2Login()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getChecklistPdf_returns401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/ipd/irgendeine-id/checklist-pdf"))
                .andExpect(status().isUnauthorized());
    }
}
