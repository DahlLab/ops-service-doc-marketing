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

    private IpdDocument buildEmptyDocument(String id, String ticketId) {
        return new IpdDocument(
                id, ticketId, IpdDocumentStatus.DRAFT, "Server maintenance", "Jane Doe",
                ScenarioType.SERVER_MAINTENANCE, null, null, null, null, null, null, null, null,
                null, null, null, "", null, null, null, false,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void getAllIpdDocuments_returnsAllDocuments() {
        when(ipdDocumentRepository.findAll()).thenReturn(List.of(buildEmptyDocument("doc-1", "ticket-1")));

        List<IpdDocumentDto> result = ipdDocumentService.getAllIpdDocuments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Server maintenance");
    }

    @Test
    void getIpdDocumentById_returnsDocument_whenIdExists() {
        when(ipdDocumentRepository.findById("doc-1")).thenReturn(Optional.of(buildEmptyDocument("doc-1", "ticket-1")));

        IpdDocumentDto result = ipdDocumentService.getIpdDocumentById("doc-1");

        assertThat(result.title()).isEqualTo("Server maintenance");
    }

    @Test
    void getIpdDocumentById_throwsException_whenIdDoesNotExist() {
        when(ipdDocumentRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ipdDocumentService.getIpdDocumentById("unknown"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createIpdDocumentFromTicket_copiesDataFromTicket() {
        Ticket ticket = new Ticket("ticket-1", null, "Server maintenance", "Description",
                TicketStatus.IN_PROGRESS, "Jane Doe", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(taskRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(ipdDocumentRepository.save(any(IpdDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IpdDocumentDto result = ipdDocumentService.createIpdDocumentFromTicket("ticket-1");

        assertThat(result.title()).isEqualTo("Server maintenance");
        assertThat(result.technician()).isEqualTo("Jane Doe");
        assertThat(result.scenarioType()).isEqualTo(ScenarioType.SERVER_MAINTENANCE);
        assertThat(result.status()).isEqualTo(IpdDocumentStatus.DRAFT);
    }

    @Test
    void createIpdDocumentFromTicket_throwsException_whenTicketDoesNotExist() {
        when(ticketRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ipdDocumentService.createIpdDocumentFromTicket("unknown"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createIpdDocumentFromTicket_setsQualityAssuranceCompleted_whenAllChecklistsCompleted() {
        Ticket ticket = new Ticket("ticket-1", null, "Server maintenance", "Description",
                TicketStatus.IN_PROGRESS, "Jane Doe", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        ChecklistItem item = new ChecklistItem("item-1", "UPS checked", true);
        Checklist completedChecklist = new Checklist("checklist-1", "ticket-1", "Checklist",
                List.of(item), LocalDateTime.now(), LocalDateTime.now());
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(taskRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of(completedChecklist));
        when(ipdDocumentRepository.save(any(IpdDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IpdDocumentDto result = ipdDocumentService.createIpdDocumentFromTicket("ticket-1");

        assertThat(result.qualityAssuranceCompleted()).isTrue();
    }

    @Test
    void createIpdDocumentFromTicket_setsQualityAssuranceNotCompleted_whenNoChecklistsExist() {
        Ticket ticket = new Ticket("ticket-1", null, "Server maintenance", "Description",
                TicketStatus.IN_PROGRESS, "Jane Doe", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(taskRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(ipdDocumentRepository.save(any(IpdDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IpdDocumentDto result = ipdDocumentService.createIpdDocumentFromTicket("ticket-1");

        assertThat(result.qualityAssuranceCompleted()).isFalse();
    }

    @Test
    void updateIpdDocument_updatesManualFieldsAndKeepsCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        IpdDocument existingDocument = new IpdDocument(
                "doc-1", "ticket-1", IpdDocumentStatus.DRAFT, "Server maintenance", "Jane Doe",
                ScenarioType.SERVER_MAINTENANCE, null, null, null, null, null, null, null, null,
                null, null, null, "", null, null, null, false, createdAt, createdAt);
        when(ipdDocumentRepository.findById("doc-1")).thenReturn(Optional.of(existingDocument));
        when(taskRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of());
        when(ipdDocumentRepository.save(any(IpdDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IpdDocumentDto change = new IpdDocumentDto(
                null, "ticket-1", IpdDocumentStatus.COMPLETED, "Server maintenance", "Jane Doe",
                ScenarioType.SERVER_MAINTENANCE, "Example Corp", "Mr Example", "14.09. - 16.09.2026",
                "UPS failed", "Install new UPS", "A server room", "1x Hyper-V host", "A VLAN",
                "Technician on site", "Daily backup", "Access via VPN only", null,
                "UPS model X selected", "Power outage during maintenance", "Rollback to old UPS possible",
                false, null, null);

        IpdDocumentDto result = ipdDocumentService.updateIpdDocument("doc-1", change);

        assertThat(result.status()).isEqualTo(IpdDocumentStatus.COMPLETED);
        assertThat(result.customer()).isEqualTo("Example Corp");
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.updatedAt()).isNotEqualTo(createdAt);
    }

    @Test
    void updateIpdDocument_throwsException_whenIdDoesNotExist() {
        when(ipdDocumentRepository.findById("unknown")).thenReturn(Optional.empty());
        IpdDocumentDto change = new IpdDocumentDto(
                null, "ticket-1", IpdDocumentStatus.COMPLETED, "Server maintenance", null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, null, null);

        assertThatThrownBy(() -> ipdDocumentService.updateIpdDocument("unknown", change))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteIpdDocument_deletesDocument_whenIdExists() {
        when(ipdDocumentRepository.existsById("doc-1")).thenReturn(true);

        ipdDocumentService.deleteIpdDocument("doc-1");

        verify(ipdDocumentRepository).deleteById("doc-1");
    }

    @Test
    void deleteIpdDocument_throwsException_whenIdDoesNotExist() {
        when(ipdDocumentRepository.existsById("unknown")).thenReturn(false);

        assertThatThrownBy(() -> ipdDocumentService.deleteIpdDocument("unknown"))
                .isInstanceOf(NoSuchElementException.class);

        verify(ipdDocumentRepository, never()).deleteById(any());
    }

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
        Checklist checklist = new Checklist("cl-1", "ticket-1", "Maintenance",
                List.of(new ChecklistItem("i-1", "UPS checked", true), new ChecklistItem("i-2", "Backup checked", false)),
                LocalDateTime.now(), null);
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of(checklist));

        byte[] result = ipdDocumentService.generateChecklistPdf("doc-1");

        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");

        assertThat(new String(result, java.nio.charset.StandardCharsets.ISO_8859_1)).contains("/AcroForm");
    }

    @Test
    void generateChecklistPdf_throwsNoSuchElement_whenNoChecklistExists() {
        when(ipdDocumentRepository.findById("doc-1")).thenReturn(Optional.of(buildEmptyDocument("doc-1", "ticket-1")));
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of());

        assertThatThrownBy(() -> ipdDocumentService.generateChecklistPdf("doc-1"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("no checklist");
    }
}
