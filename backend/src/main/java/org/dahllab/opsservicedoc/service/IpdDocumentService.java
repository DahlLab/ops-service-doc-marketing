package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.IpdDocumentDto;
import org.dahllab.opsservicedoc.model.Checklist;
import org.dahllab.opsservicedoc.model.IpdDocument;
import org.dahllab.opsservicedoc.model.IpdDocumentStatus;
import org.dahllab.opsservicedoc.model.Task;
import org.dahllab.opsservicedoc.model.TaskStatus;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.repository.ChecklistRepository;
import org.dahllab.opsservicedoc.repository.IpdDocumentRepository;
import org.dahllab.opsservicedoc.repository.TaskRepository;
import org.dahllab.opsservicedoc.repository.TicketRepository;
import org.dahllab.opsservicedoc.dto.ChecklistDto;
import org.dahllab.opsservicedoc.util.ChecklistMapper;
import org.dahllab.opsservicedoc.util.ChecklistPdfGenerator;
import org.dahllab.opsservicedoc.util.IpdDocumentMapper;
import org.dahllab.opsservicedoc.util.IpdPdfGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class IpdDocumentService {
    private static final String RESOURCE_NOT_FOUND = " nicht gefunden";

    private final IpdDocumentRepository ipdDocumentRepository;
    private final TicketRepository ticketRepository;
    private final TaskRepository taskRepository;
    private final ChecklistRepository checklistRepository;

    public IpdDocumentService(IpdDocumentRepository ipdDocumentRepository,
                              TicketRepository ticketRepository,
                              TaskRepository taskRepository,
                              ChecklistRepository checklistRepository) {
        this.ipdDocumentRepository = ipdDocumentRepository;
        this.ticketRepository = ticketRepository;
        this.taskRepository = taskRepository;
        this.checklistRepository = checklistRepository;
    }

    public List<IpdDocumentDto> getAllIpdDocuments() {
        return ipdDocumentRepository.findAll().stream()
                .map(IpdDocumentMapper::toDto)
                .toList();
    }

    public List<IpdDocumentDto> getIpdDocumentsByTicketId(String ticketId) {
        return ipdDocumentRepository.findByTicketId(ticketId).stream()
                .map(IpdDocumentMapper::toDto)
                .toList();
    }

    public IpdDocumentDto getIpdDocumentById(String id) {
        IpdDocument result = ipdDocumentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("IPD-Dokument mit ID " + id + RESOURCE_NOT_FOUND));
        return IpdDocumentMapper.toDto(result);
    }

    public IpdDocumentDto createIpdDocumentFromTicket(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket mit ID " + ticketId + RESOURCE_NOT_FOUND));

        IpdDocument newDocument = new IpdDocument(
                null,
                ticketId,
                IpdDocumentStatus.DRAFT,
                ticket.getTitle(),
                ticket.getTechnician(),
                ticket.getScenarioType(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                buildPerformedStepsText(ticketId),
                null,
                null,
                null,
                determineQualityAssuranceCompleted(ticketId),
                LocalDateTime.now(ZoneId.systemDefault()),
                LocalDateTime.now(ZoneId.systemDefault())
        );

        IpdDocument result = ipdDocumentRepository.save(newDocument);
        return IpdDocumentMapper.toDto(result);
    }

    public IpdDocumentDto updateIpdDocument(String id, IpdDocumentDto ipdDocumentDto) {
        IpdDocument existingDocument = ipdDocumentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("IPD-Dokument mit ID " + id + RESOURCE_NOT_FOUND));

        IpdDocument updatedDocument = new IpdDocument(
                id,
                existingDocument.getTicketId(),
                ipdDocumentDto.status() != null ? ipdDocumentDto.status() : existingDocument.getStatus(),
                ipdDocumentDto.title(),
                existingDocument.getTechnician(),
                existingDocument.getScenarioType(),
                ipdDocumentDto.customer(),
                ipdDocumentDto.customerContact(),
                ipdDocumentDto.period(),
                ipdDocumentDto.initialSituation(),
                ipdDocumentDto.requirements(),
                ipdDocumentDto.infrastructureOverview(),
                ipdDocumentDto.serversAndVms(),
                ipdDocumentDto.network(),
                ipdDocumentDto.rolesAndResponsibilities(),
                ipdDocumentDto.backupPlan(),
                ipdDocumentDto.securityConsiderations(),
                buildPerformedStepsText(existingDocument.getTicketId()),
                ipdDocumentDto.decisions(),
                ipdDocumentDto.risksAndAssumptions(),
                ipdDocumentDto.rollbackPlan(),
                determineQualityAssuranceCompleted(existingDocument.getTicketId()),
                existingDocument.getCreatedAt(),
                LocalDateTime.now(ZoneId.systemDefault())
        );

        IpdDocument result = ipdDocumentRepository.save(updatedDocument);
        return IpdDocumentMapper.toDto(result);
    }

    public void deleteIpdDocument(String id) {
        if (!ipdDocumentRepository.existsById(id)) {
            throw new NoSuchElementException("IPD-Dokument mit ID " + id + RESOURCE_NOT_FOUND);
        }
        ipdDocumentRepository.deleteById(id);
    }

    public byte[] generatePdf(String id) {
        IpdDocumentDto ipdDocument = getIpdDocumentById(id);
        return IpdPdfGenerator.createPdf(ipdDocument);
    }

    public byte[] generateChecklistPdf(String id) {
        IpdDocumentDto ipdDocument = getIpdDocumentById(id);
        List<ChecklistDto> checklists = checklistRepository.findByTicketId(ipdDocument.ticketId()).stream()
                .map(ChecklistMapper::toDto)
                .toList();
        if (checklists.isEmpty()) {
            throw new NoSuchElementException("Zu diesem IPD-Dokument gibt es keine Checkliste");
        }
        return ChecklistPdfGenerator.createPdf(ipdDocument, checklists);
    }

    private boolean determineQualityAssuranceCompleted(String ticketId) {
        List<Checklist> checklists = checklistRepository.findByTicketId(ticketId);
        return !checklists.isEmpty() && checklists.stream().allMatch(checklist -> checklist.getCompletedAt() != null);
    }

    private String buildPerformedStepsText(String ticketId) {
        List<Task> doneTasks = taskRepository.findByTicketId(ticketId).stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .toList();

        if (doneTasks.isEmpty()) {
            return "";
        }

        return doneTasks.stream()
                .map(task -> "- " + task.getTopic()
                        + (task.getNextSteps() != null && !task.getNextSteps().isBlank()
                        ? ": " + task.getNextSteps()
                        : ""))
                .collect(Collectors.joining("\n"));
    }
}
