package org.dahllab.opsservicedoc.controller;

import org.dahllab.opsservicedoc.model.Checklist;
import org.dahllab.opsservicedoc.model.ChecklistItem;
import org.dahllab.opsservicedoc.model.IpdDocument;
import org.dahllab.opsservicedoc.model.IpdDocumentStatus;
import org.dahllab.opsservicedoc.model.SzenarioTyp;
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

// @SpringBootTest + @AutoConfigureMockMvc: startet den kompletten
// Anwendungskontext und stellt MockMvc bereit, gleiches Muster wie
// ChecklistControllerTest.
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

    // Ich leere vor jedem Test alle beteiligten Collections, damit die
    // Tests unabhängig voneinander laufen - IpdDocument hängt
    // schließlich von Ticket/Task/Checklist ab.
    @BeforeEach
    void setUp() {
        ipdDocumentRepository.deleteAll();
        ticketRepository.deleteAll();
        taskRepository.deleteAll();
        checklistRepository.deleteAll();
    }

    @Test
    void getAllIpdDocuments_gibtLeereListeZurueck_wennKeineDokumenteVorhanden() throws Exception {
        mockMvc.perform(get("/api/ipd").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Prüft den Erfolgsfall von POST /api/ipd/from-ticket/{ticketId}:
    // ich lege vorher ein echtes Ticket an und erwarte, dass dessen
    // Titel und Techniker automatisch ins neue IPD-Dokument übernommen
    // werden.
    @Test
    void postIpdDocumentFromTicket_erzeugtEntwurf_undGibt201Zurueck() throws Exception {
        Ticket gespeichertesTicket = ticketRepository.save(new Ticket(
                null, null, "Server-Wartung", "Beschreibung", TicketStatus.IN_BEARBEITUNG,
                "Mia Muster", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now()));

        mockMvc.perform(post("/api/ipd/from-ticket/" + gespeichertesTicket.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titel").value("Server-Wartung"))
                .andExpect(jsonPath("$.techniker").value("Mia Muster"))
                .andExpect(jsonPath("$.status").value("ENTWURF"));
    }

    // Prüft den Fehlerfall: eine unbekannte ticketId muss zu einem
    // 4xx-Fehler führen.
    @Test
    void postIpdDocumentFromTicket_gibt404Zurueck_wennTicketNichtExistiert() throws Exception {
        mockMvc.perform(post("/api/ipd/from-ticket/unbekannt").with(oauth2Login()).with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    // Prüft den Fehlerfall von GET /api/ipd/{id}.
    @Test
    void getIpdDocumentById_gibt404_wennIdNichtExistiert() throws Exception {
        mockMvc.perform(get("/api/ipd/unbekannt").with(oauth2Login()))
                .andExpect(status().is4xxClientError());
    }

    // Prüft PUT /api/ipd/{id}: die manuell gepflegten Felder müssen
    // übernommen werden, inklusive Statuswechsel auf ABGESCHLOSSEN.
    @Test
    void putIpdDocument_aktualisiertFelderUndStatus() throws Exception {
        Ticket gespeichertesTicket = ticketRepository.save(new Ticket(
                null, null, "Server-Wartung", "Beschreibung", TicketStatus.IN_BEARBEITUNG,
                "Mia Muster", SzenarioTyp.SERVER_WARTUNG, LocalDateTime.now()));
        IpdDocument gespeichertesDokument = ipdDocumentRepository.save(new IpdDocument(
                null, gespeichertesTicket.getId(), IpdDocumentStatus.ENTWURF, "Server-Wartung",
                "Mia Muster", SzenarioTyp.SERVER_WARTUNG, null, null, null, null, null, null, null,
                null, null, null, null, "", null, null, null, false,
                LocalDateTime.now(), LocalDateTime.now()));

        String requestBody = """
                {
                    "ticketId": "%s",
                    "status": "ABGESCHLOSSEN",
                    "titel": "Server-Wartung",
                    "kunde": "Musterfirma GmbH",
                    "ansprechpartnerKunde": "Herr Beispiel",
                    "zeitraum": "14.09.2026 - 16.09.2026",
                    "ausgangslage": "USV ausgefallen",
                    "qualitaetssicherungAbgeschlossen": false
                }
                """.formatted(gespeichertesTicket.getId());

        mockMvc.perform(put("/api/ipd/" + gespeichertesDokument.getId())
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABGESCHLOSSEN"))
                .andExpect(jsonPath("$.kunde").value("Musterfirma GmbH"));
    }

    // Prüft DELETE /api/ipd/{id}: erfolgreiches Löschen muss 204 No
    // Content liefern.
    @Test
    void deleteIpdDocument_entferntDokument_undGibt204Zurueck() throws Exception {
        IpdDocument gespeichertesDokument = ipdDocumentRepository.save(new IpdDocument(
                null, "ticket-1", IpdDocumentStatus.ENTWURF, "Server-Wartung", "Mia Muster",
                SzenarioTyp.SERVER_WARTUNG, null, null, null, null, null, null, null, null, null,
                null, null, "", null, null, null, false, LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(delete("/api/ipd/" + gespeichertesDokument.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    // Prüft GET /api/ipd/{id}/pdf: Response muss den PDF-Content-Type
    // tragen.
    @Test
    void getPdf_liefertPdfDatei() throws Exception {
        IpdDocument gespeichertesDokument = ipdDocumentRepository.save(new IpdDocument(
                null, "ticket-1", IpdDocumentStatus.ENTWURF, "Server-Wartung", "Mia Muster",
                SzenarioTyp.SERVER_WARTUNG, "Musterfirma GmbH", null, null, "USV ausgefallen", null,
                null, null, null, null, null, null, "", null, null, null, false,
                LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(get("/api/ipd/" + gespeichertesDokument.getId() + "/pdf").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void getChecklistPdf_liefertPdfDatei_wennChecklisteVorhanden() throws Exception {
        IpdDocument dokument = ipdDocumentRepository.save(new IpdDocument(
                null, "ticket-1", IpdDocumentStatus.ENTWURF, "Server-Wartung", "Mia Muster",
                SzenarioTyp.SERVER_WARTUNG, "Musterfirma GmbH", null, null, null, null,
                null, null, null, null, null, null, "", null, null, null, false,
                LocalDateTime.now(), LocalDateTime.now()));
        checklistRepository.save(new Checklist(null, "ticket-1", "Wartung",
                List.of(new ChecklistItem("i-1", "USV geprüft", false)), LocalDateTime.now(), null));

        mockMvc.perform(get("/api/ipd/" + dokument.getId() + "/checklist-pdf").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"checkliste-" + dokument.getId() + ".pdf\""));
    }

    @Test
    void getChecklistPdf_gibt404_wennKeineChecklisteVorhanden() throws Exception {
        IpdDocument dokument = ipdDocumentRepository.save(new IpdDocument(
                null, "ticket-1", IpdDocumentStatus.ENTWURF, "Server-Wartung", "Mia Muster",
                SzenarioTyp.SERVER_WARTUNG, "Musterfirma GmbH", null, null, null, null,
                null, null, null, null, null, null, "", null, null, null, false,
                LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(get("/api/ipd/" + dokument.getId() + "/checklist-pdf").with(oauth2Login()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getChecklistPdf_gibt401_wennNichtEingeloggt() throws Exception {
        mockMvc.perform(get("/api/ipd/irgendeine-id/checklist-pdf"))
                .andExpect(status().isUnauthorized());
    }
}
