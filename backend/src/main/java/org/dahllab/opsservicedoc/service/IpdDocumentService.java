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

    // Gemeinsamer Teil der "nicht gefunden"-Fehlermeldungen: ein Literal statt vieler Kopien
    private static final String RESOURCE_NOT_FOUND = " nicht gefunden";

    private final IpdDocumentRepository ipdDocumentRepository;
    private final TicketRepository ticketRepository;
    private final TaskRepository taskRepository;
    private final ChecklistRepository checklistRepository;

    // Konstruktor-Injection für alle vier Repositories - ich brauche
    // Ticket/Task/Checklist nur lesend, um die automatischen Felder zu
    // befüllen bzw. neu zu berechnen.
    public IpdDocumentService(IpdDocumentRepository ipdDocumentRepository,
                              TicketRepository ticketRepository,
                              TaskRepository taskRepository,
                              ChecklistRepository checklistRepository) {
        this.ipdDocumentRepository = ipdDocumentRepository;
        this.ticketRepository = ticketRepository;
        this.taskRepository = taskRepository;
        this.checklistRepository = checklistRepository;
    }

    // GET /api/ipd - liefert alle IPD-Dokumente.
    public List<IpdDocumentDto> getAllIpdDocuments() {
        return ipdDocumentRepository.findAll().stream()
                .map(IpdDocumentMapper::toDto)
                .toList();
    }

    // GET /api/ipd?ticketId=... - liefert nur die IPD-Dokumente zu
    // einem bestimmten Ticket.
    public List<IpdDocumentDto> getIpdDocumentsByTicketId(String ticketId) {
        return ipdDocumentRepository.findByTicketId(ticketId).stream()
                .map(IpdDocumentMapper::toDto)
                .toList();
    }

    // GET /api/ipd/{id} - liefert genau ein IPD-Dokument.
    public IpdDocumentDto getIpdDocumentById(String id) {
        IpdDocument result = ipdDocumentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("IPD-Dokument mit ID " + id + RESOURCE_NOT_FOUND));
        return IpdDocumentMapper.toDto(result);
    }

    // POST /api/ipd/from-ticket/{ticketId} - erzeugt einen neuen
    // IPD-Entwurf automatisch aus einem bestehenden Ticket: Titel,
    // Techniker und Szenario übernehme ich direkt vom Ticket, die
    // bereits erledigten Tasks fasse ich zu einem Fließtext zusammen,
    // und ich ermittle, ob die interne Qualitätssicherung
    // (Checklisten) schon abgeschlossen ist. Alle restlichen
    // Abschnitte (Kunde, Ausgangslage, Anforderungen, ...) bleiben
    // zunächst leer und müssen von mir per PUT ergänzt werden.
    public IpdDocumentDto createIpdDocumentFromTicket(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket mit ID " + ticketId + RESOURCE_NOT_FOUND));

        IpdDocument newDocument = new IpdDocument(
                null,                                                  // id
                ticketId,                                              // ticketId
                IpdDocumentStatus.DRAFT,                             // status
                ticket.getTitle(),                                     // titel
                ticket.getTechnician(),                                 // techniker
                ticket.getScenarioType(),                               // szenarioTyp
                null,                                                  // kunde
                null,                                                  // ansprechpartnerKunde
                null,                                                  // zeitraum
                null,                                                  // ausgangslage
                null,                                                  // anforderungen
                null,                                                  // infrastrukturUebersicht
                null,                                                  // serverUndVms
                null,                                                  // netzwerk
                null,                                                  // rollenUndVerantwortlichkeiten
                null,                                                  // backupKonzept
                null,                                                  // securityUeberlegungen
                buildPerformedStepsText(ticketId),             // durchgefuehrteSchritte
                null,                                                  // entscheidungen
                null,                                                  // risikenUndAnnahmen
                null,                                                  // rollbackPlan
                determineQualityAssuranceCompleted(ticketId),   // qualitaetssicherungAbgeschlossen
                LocalDateTime.now(ZoneId.systemDefault()),                                   // erstelltAm
                LocalDateTime.now(ZoneId.systemDefault())                                    // aktualisiertAm
        );

        IpdDocument result = ipdDocumentRepository.save(newDocument);
        return IpdDocumentMapper.toDto(result);
    }

    // PUT /api/ipd/{id} - aktualisiert die manuell gepflegten
    // Abschnitte eines bestehenden IPD-Dokuments (z.B. um es auf
    // ABGESCHLOSSEN zu setzen, bevor es an den Kunden geht).
    // ticketId, techniker, szenarioTyp und erstelltAm bleiben
    // unverändert, durchgefuehrteSchritte und
    // qualitaetssicherungAbgeschlossen werden bei jedem Update neu aus
    // dem aktuellen Stand von Task/Checklist berechnet statt vom
    // Client übernommen zu werden - so bleiben sie immer aktuell, auch
    // wenn inzwischen weitere Tasks erledigt oder Checklisten
    // abgeschlossen wurden.
    public IpdDocumentDto updateIpdDocument(String id, IpdDocumentDto ipdDocumentDto) {
        IpdDocument existingDocument = ipdDocumentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("IPD-Dokument mit ID " + id + RESOURCE_NOT_FOUND));

        IpdDocument updatedDocument = new IpdDocument(
                id,                                                                            // id
                existingDocument.getTicketId(),                                            // ticketId
                ipdDocumentDto.status() != null ? ipdDocumentDto.status() : existingDocument.getStatus(), // status
                ipdDocumentDto.title(),                                                        // titel
                existingDocument.getTechnician(),                                            // techniker
                existingDocument.getScenarioType(),                                          // szenarioTyp
                ipdDocumentDto.customer(),                                                        // kunde
                ipdDocumentDto.customerContact(),                                         // ansprechpartnerKunde
                ipdDocumentDto.period(),                                                     // zeitraum
                ipdDocumentDto.initialSituation(),                                                 // ausgangslage
                ipdDocumentDto.requirements(),                                                // anforderungen
                ipdDocumentDto.infrastructureOverview(),                                      // infrastrukturUebersicht
                ipdDocumentDto.serversAndVms(),                                                 // serverUndVms
                ipdDocumentDto.network(),                                                     // netzwerk
                ipdDocumentDto.rolesAndResponsibilities(),                                // rollenUndVerantwortlichkeiten
                ipdDocumentDto.backupPlan(),                                                // backupKonzept
                ipdDocumentDto.securityConsiderations(),                                        // securityUeberlegungen
                buildPerformedStepsText(existingDocument.getTicketId()),            // durchgefuehrteSchritte
                ipdDocumentDto.decisions(),                                               // entscheidungen
                ipdDocumentDto.risksAndAssumptions(),                                           // risikenUndAnnahmen
                ipdDocumentDto.rollbackPlan(),                                                 // rollbackPlan
                determineQualityAssuranceCompleted(existingDocument.getTicketId()),  // qualitaetssicherungAbgeschlossen
                existingDocument.getCreatedAt(),                                           // erstelltAm bleibt unverändert
                LocalDateTime.now(ZoneId.systemDefault())                                                            // aktualisiertAm
        );

        IpdDocument result = ipdDocumentRepository.save(updatedDocument);
        return IpdDocumentMapper.toDto(result);
    }

    // DELETE /api/ipd/{id} - löscht ein IPD-Dokument.
    public void deleteIpdDocument(String id) {
        if (!ipdDocumentRepository.existsById(id)) {
            throw new NoSuchElementException("IPD-Dokument mit ID " + id + RESOURCE_NOT_FOUND);
        }
        ipdDocumentRepository.deleteById(id);
    }

    // GET /api/ipd/{id}/pdf - lädt das Dokument und lässt daraus das
    // fertige PDF erzeugen (siehe IpdPdfGenerator).
    public byte[] generatePdf(String id) {
        IpdDocumentDto ipdDocument = getIpdDocumentById(id);
        return IpdPdfGenerator.createPdf(ipdDocument);
    }

    // GET /api/ipd/{id}/checklist-pdf - interne Technikerversion der
    // Checklisten zu diesem Dokument (druckbar / am Tablet ausfüllbar).
    // Ohne Checkliste gibt es nichts zu exportieren -> 404.
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

    // Ermittelt automatisch, ob die interne Qualitätssicherung für
    // dieses Ticket abgeschlossen ist: true nur dann, wenn mindestens
    // eine Checkliste zu diesem Ticket existiert UND wirklich ALLE
    // davon abgeschlossen sind (abgeschlossenAm gesetzt). Ohne
    // Checklisten bleibt es false, auch wenn es "nichts zu erledigen
    // gab" - sonst würde ein Ticket ohne jede Checkliste fälschlich so
    // aussehen, als wäre die QS schon durchgeführt.
    private boolean determineQualityAssuranceCompleted(String ticketId) {
        List<Checklist> checklists = checklistRepository.findByTicketId(ticketId);
        return !checklists.isEmpty() && checklists.stream().allMatch(checklist -> checklist.getCompletedAt() != null);
    }

    // Baut aus allen ERLEDIGTEN Tasks des Tickets einen lesbaren
    // Fließtext für den Abschnitt "Durchgeführte Schritte" zusammen -
    // eine Zeile pro Task, bestehend aus Thema und (falls vorhanden)
    // den nächsten Schritten/Kommentar. So muss ich die bereits in
    // TaskPlanner erfassten Arbeitsschritte nicht ein zweites Mal von
    // Hand eintippen.
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