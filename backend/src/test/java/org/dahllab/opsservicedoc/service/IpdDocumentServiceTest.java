package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.IpdDocumentDto;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit-Tests für IpdDocumentService mit gemockten Repositories, im
// selben Stil wie ChecklistServiceTest: ich teste hier nur die
// Service-Logik isoliert, ohne echte MongoDB-Anbindung.
@ExtendWith(MockitoExtension.class)
class IpdDocumentServiceTest {

    @Mock
    private IpdDocumentRepository ipdDocumentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ChecklistRepository checklistRepository;

    @InjectMocks
    private IpdDocumentService ipdDocumentService;

    // Baut ein vollständiges, aber inhaltlich leeres IpdDocument für
    // Tests, die nur ein paar bestimmte Felder brauchen - vermeidet,
    // dass ich den 24-Argumente-Konstruktor in jedem Test komplett neu
    // hinschreiben muss.
    private IpdDocument buildEmptyDocument(String id, String ticketId) {
        return new IpdDocument(
                id, ticketId, IpdDocumentStatus.DRAFT, "Server-Wartung", "Mia Muster",
                ScenarioType.SERVER_MAINTENANCE, null, null, null, null, null, null, null, null,
                null, null, null, "", null, null, null, false,
                LocalDateTime.now(), LocalDateTime.now());
    }

    // Prüft, dass getAllIpdDocuments() alle gefundenen Dokumente als
    // DTOs zurückgibt.
    @Test
    void getAllIpdDocuments_returnsAllDocuments() {
        when(ipdDocumentRepository.findAll()).thenReturn(List.of(buildEmptyDocument("doc-1", "ticket-1")));

        List<IpdDocumentDto> result = ipdDocumentService.getAllIpdDocuments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Server-Wartung");
    }

    // Prüft den Erfolgsfall von getIpdDocumentById().
    @Test
    void getIpdDocumentById_returnsDocument_whenIdExists() {
        when(ipdDocumentRepository.findById("doc-1")).thenReturn(Optional.of(buildEmptyDocument("doc-1", "ticket-1")));

        IpdDocumentDto result = ipdDocumentService.getIpdDocumentById("doc-1");

        assertThat(result.title()).isEqualTo("Server-Wartung");
    }

    // Prüft, dass eine unbekannte ID bei getIpdDocumentById() zu einer
    // NoSuchElementException führt.
    @Test
    void getIpdDocumentById_throwsException_whenIdDoesNotExist() {
        when(ipdDocumentRepository.findById("unbekannt")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ipdDocumentService.getIpdDocumentById("unbekannt"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // Prüft, dass createIpdDocumentFromTicket() Titel, Techniker und
    // Szenario automatisch aus dem Ticket übernimmt.
    @Test
    void createIpdDocumentFromTicket_copiesDataFromTicket() {
        Ticket ticket = new Ticket("ticket-1", null, "Server-Wartung", "Beschreibung",
                TicketStatus.IN_PROGRESS, "Mia Muster", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(taskRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(ipdDocumentRepository.save(any(IpdDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IpdDocumentDto result = ipdDocumentService.createIpdDocumentFromTicket("ticket-1");

        assertThat(result.title()).isEqualTo("Server-Wartung");
        assertThat(result.technician()).isEqualTo("Mia Muster");
        assertThat(result.scenarioType()).isEqualTo(ScenarioType.SERVER_MAINTENANCE);
        assertThat(result.status()).isEqualTo(IpdDocumentStatus.DRAFT);
    }

    // Prüft, dass eine unbekannte ticketId eine NoSuchElementException
    // wirft.
    @Test
    void createIpdDocumentFromTicket_throwsException_whenTicketDoesNotExist() {
        when(ticketRepository.findById("unbekannt")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ipdDocumentService.createIpdDocumentFromTicket("unbekannt"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // Prüft, dass qualitaetssicherungAbgeschlossen auf true gesetzt
    // wird, wenn alle Checklisten des Tickets abgeschlossen sind.
    @Test
    void createIpdDocumentFromTicket_setsQualityAssuranceCompleted_whenAllChecklistsCompleted() {
        Ticket ticket = new Ticket("ticket-1", null, "Server-Wartung", "Beschreibung",
                TicketStatus.IN_PROGRESS, "Mia Muster", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        ChecklistItem item = new ChecklistItem("item-1", "USV geprüft", true);
        Checklist completedChecklist = new Checklist("checklist-1", "ticket-1", "Checkliste",
                List.of(item), LocalDateTime.now(), LocalDateTime.now());
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(taskRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of(completedChecklist));
        when(ipdDocumentRepository.save(any(IpdDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IpdDocumentDto result = ipdDocumentService.createIpdDocumentFromTicket("ticket-1");

        assertThat(result.qualityAssuranceCompleted()).isTrue();
    }

    // Prüft die Kehrseite: ohne vorhandene Checklisten darf
    // qualitaetssicherungAbgeschlossen nicht automatisch auf true
    // stehen, auch wenn es "nichts zu erledigen gab".
    @Test
    void createIpdDocumentFromTicket_setsQualityAssuranceNotCompleted_whenNoChecklistsExist() {
        Ticket ticket = new Ticket("ticket-1", null, "Server-Wartung", "Beschreibung",
                TicketStatus.IN_PROGRESS, "Mia Muster", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(taskRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(ipdDocumentRepository.save(any(IpdDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IpdDocumentDto result = ipdDocumentService.createIpdDocumentFromTicket("ticket-1");

        assertThat(result.qualityAssuranceCompleted()).isFalse();
    }

    // Prüft, dass updateIpdDocument() die manuell gepflegten Felder
    // übernimmt, erstelltAm unverändert lässt und aktualisiertAm neu
    // setzt.
    @Test
    void updateIpdDocument_updatesManualFieldsAndKeepsCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        IpdDocument existingDocument = new IpdDocument(
                "doc-1", "ticket-1", IpdDocumentStatus.DRAFT, "Server-Wartung", "Mia Muster",
                ScenarioType.SERVER_MAINTENANCE, null, null, null, null, null, null, null, null,
                null, null, null, "", null, null, null, false, createdAt, createdAt);
        when(ipdDocumentRepository.findById("doc-1")).thenReturn(Optional.of(existingDocument));
        when(taskRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(ipdDocumentRepository.save(any(IpdDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IpdDocumentDto change = new IpdDocumentDto(
                null, "ticket-1", IpdDocumentStatus.COMPLETED, "Server-Wartung", "Mia Muster",
                ScenarioType.SERVER_MAINTENANCE, "Musterfirma GmbH", "Herr Beispiel", "14.09. - 16.09.2026",
                "USV ausgefallen", "Neue USV einbauen", "Ein Serverraum", "1x Hyper-V-Host", "Ein VLAN",
                "Techniker vor Ort", "Tägliches Backup", "Zugriff nur per VPN", null,
                "USV-Modell X gewählt", "Stromausfall während Wartung", "Rollback auf alte USV möglich",
                false, null, null);

        IpdDocumentDto result = ipdDocumentService.updateIpdDocument("doc-1", change);

        assertThat(result.status()).isEqualTo(IpdDocumentStatus.COMPLETED);
        assertThat(result.customer()).isEqualTo("Musterfirma GmbH");
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.updatedAt()).isNotEqualTo(createdAt);
    }

    // Prüft, dass ein Update auf eine unbekannte ID eine
    // NoSuchElementException wirft.
    @Test
    void updateIpdDocument_throwsException_whenIdDoesNotExist() {
        when(ipdDocumentRepository.findById("unbekannt")).thenReturn(Optional.empty());
        IpdDocumentDto change = new IpdDocumentDto(
                null, "ticket-1", IpdDocumentStatus.COMPLETED, "Server-Wartung", null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, null, null);

        assertThatThrownBy(() -> ipdDocumentService.updateIpdDocument("unbekannt", change))
                .isInstanceOf(NoSuchElementException.class);
    }

    // Prüft den Erfolgsfall von deleteIpdDocument().
    @Test
    void deleteIpdDocument_deletesDocument_whenIdExists() {
        when(ipdDocumentRepository.existsById("doc-1")).thenReturn(true);

        ipdDocumentService.deleteIpdDocument("doc-1");

        verify(ipdDocumentRepository).deleteById("doc-1");
    }

    // Prüft, dass ein Löschversuch auf eine unbekannte ID eine
    // NoSuchElementException wirft, statt dass die Repository-Methode
    // still nichts tut.
    @Test
    void deleteIpdDocument_throwsException_whenIdDoesNotExist() {
        when(ipdDocumentRepository.existsById("unbekannt")).thenReturn(false);

        assertThatThrownBy(() -> ipdDocumentService.deleteIpdDocument("unbekannt"))
                .isInstanceOf(NoSuchElementException.class);

        verify(ipdDocumentRepository, never()).deleteById(any());
    }

    // Prüft, dass generatePdf() tatsächlich Bytes liefert, die mit der
    // PDF-Kennung "%PDF" beginnen - ich prüfe bewusst nicht den
    // kompletten Inhalt, nur dass wirklich ein echtes PDF entsteht.
    @Test
    void generatePdf_createsNonEmptyPdf() {
        when(ipdDocumentRepository.findById("doc-1")).thenReturn(Optional.of(buildEmptyDocument("doc-1", "ticket-1")));

        byte[] result = ipdDocumentService.generatePdf("doc-1");

        assertThat(result).isNotEmpty();
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generateChecklistPdf_createsPdf_whenChecklistExists() {
        when(ipdDocumentRepository.findById("doc-1")).thenReturn(Optional.of(buildEmptyDocument("doc-1", "ticket-1")));
        Checklist checklist = new Checklist("cl-1", "ticket-1", "Wartung",
                List.of(new ChecklistItem("i-1", "USV geprüft", true), new ChecklistItem("i-2", "Backup geprüft", false)),
                LocalDateTime.now(), null);
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of(checklist));

        byte[] result = ipdDocumentService.generateChecklistPdf("doc-1");

        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
        // Die Checkboxen sind echte Formularfelder (AcroForm).
        assertThat(new String(result, java.nio.charset.StandardCharsets.ISO_8859_1)).contains("/AcroForm");
    }

    @Test
    void generateChecklistPdf_throwsNoSuchElement_whenNoChecklistExists() {
        when(ipdDocumentRepository.findById("doc-1")).thenReturn(Optional.of(buildEmptyDocument("doc-1", "ticket-1")));
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of());

        assertThatThrownBy(() -> ipdDocumentService.generateChecklistPdf("doc-1"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("keine Checkliste");
    }
}
