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
    private static final String NICHT_GEFUNDEN = " nicht gefunden";

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
                .orElseThrow(() -> new NoSuchElementException("IPD-Dokument mit ID " + id + NICHT_GEFUNDEN));
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
                .orElseThrow(() -> new NoSuchElementException("Ticket mit ID " + ticketId + NICHT_GEFUNDEN));

        IpdDocument neuesDokument = new IpdDocument(
                null,                                                  // id
                ticketId,                                              // ticketId
                IpdDocumentStatus.ENTWURF,                             // status
                ticket.getTitel(),                                     // titel
                ticket.getTechniker(),                                 // techniker
                ticket.getSzenarioTyp(),                               // szenarioTyp
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
                baueDurchgefuehrteSchritteText(ticketId),             // durchgefuehrteSchritte
                null,                                                  // entscheidungen
                null,                                                  // risikenUndAnnahmen
                null,                                                  // rollbackPlan
                ermittleQualitaetssicherungAbgeschlossen(ticketId),   // qualitaetssicherungAbgeschlossen
                LocalDateTime.now(ZoneId.systemDefault()),                                   // erstelltAm
                LocalDateTime.now(ZoneId.systemDefault())                                    // aktualisiertAm
        );

        IpdDocument result = ipdDocumentRepository.save(neuesDokument);
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
        IpdDocument bestehendesDokument = ipdDocumentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("IPD-Dokument mit ID " + id + NICHT_GEFUNDEN));

        IpdDocument aktualisiertesDokument = new IpdDocument(
                id,                                                                            // id
                bestehendesDokument.getTicketId(),                                            // ticketId
                ipdDocumentDto.status() != null ? ipdDocumentDto.status() : bestehendesDokument.getStatus(), // status
                ipdDocumentDto.titel(),                                                        // titel
                bestehendesDokument.getTechniker(),                                            // techniker
                bestehendesDokument.getSzenarioTyp(),                                          // szenarioTyp
                ipdDocumentDto.kunde(),                                                        // kunde
                ipdDocumentDto.ansprechpartnerKunde(),                                         // ansprechpartnerKunde
                ipdDocumentDto.zeitraum(),                                                     // zeitraum
                ipdDocumentDto.ausgangslage(),                                                 // ausgangslage
                ipdDocumentDto.anforderungen(),                                                // anforderungen
                ipdDocumentDto.infrastrukturUebersicht(),                                      // infrastrukturUebersicht
                ipdDocumentDto.serverUndVms(),                                                 // serverUndVms
                ipdDocumentDto.netzwerk(),                                                     // netzwerk
                ipdDocumentDto.rollenUndVerantwortlichkeiten(),                                // rollenUndVerantwortlichkeiten
                ipdDocumentDto.backupKonzept(),                                                // backupKonzept
                ipdDocumentDto.securityUeberlegungen(),                                        // securityUeberlegungen
                baueDurchgefuehrteSchritteText(bestehendesDokument.getTicketId()),            // durchgefuehrteSchritte
                ipdDocumentDto.entscheidungen(),                                               // entscheidungen
                ipdDocumentDto.risikenUndAnnahmen(),                                           // risikenUndAnnahmen
                ipdDocumentDto.rollbackPlan(),                                                 // rollbackPlan
                ermittleQualitaetssicherungAbgeschlossen(bestehendesDokument.getTicketId()),  // qualitaetssicherungAbgeschlossen
                bestehendesDokument.getErstelltAm(),                                           // erstelltAm bleibt unverändert
                LocalDateTime.now(ZoneId.systemDefault())                                                            // aktualisiertAm
        );

        IpdDocument result = ipdDocumentRepository.save(aktualisiertesDokument);
        return IpdDocumentMapper.toDto(result);
    }

    // DELETE /api/ipd/{id} - löscht ein IPD-Dokument.
    public void deleteIpdDocument(String id) {
        if (!ipdDocumentRepository.existsById(id)) {
            throw new NoSuchElementException("IPD-Dokument mit ID " + id + NICHT_GEFUNDEN);
        }
        ipdDocumentRepository.deleteById(id);
    }

    // GET /api/ipd/{id}/pdf - lädt das Dokument und lässt daraus das
    // fertige PDF erzeugen (siehe IpdPdfGenerator).
    public byte[] generatePdf(String id) {
        IpdDocumentDto dokument = getIpdDocumentById(id);
        return IpdPdfGenerator.erzeugePdf(dokument);
    }

    // GET /api/ipd/{id}/checklist-pdf - interne Technikerversion der
    // Checklisten zu diesem Dokument (druckbar / am Tablet ausfüllbar).
    // Ohne Checkliste gibt es nichts zu exportieren -> 404.
    public byte[] generateChecklistPdf(String id) {
        IpdDocumentDto dokument = getIpdDocumentById(id);
        List<ChecklistDto> checklisten = checklistRepository.findByTicketId(dokument.ticketId()).stream()
                .map(ChecklistMapper::toDto)
                .toList();
        if (checklisten.isEmpty()) {
            throw new NoSuchElementException("Zu diesem IPD-Dokument gibt es keine Checkliste");
        }
        return ChecklistPdfGenerator.erzeugePdf(dokument, checklisten);
    }

    // Ermittelt automatisch, ob die interne Qualitätssicherung für
    // dieses Ticket abgeschlossen ist: true nur dann, wenn mindestens
    // eine Checkliste zu diesem Ticket existiert UND wirklich ALLE
    // davon abgeschlossen sind (abgeschlossenAm gesetzt). Ohne
    // Checklisten bleibt es false, auch wenn es "nichts zu erledigen
    // gab" - sonst würde ein Ticket ohne jede Checkliste fälschlich so
    // aussehen, als wäre die QS schon durchgeführt.
    private boolean ermittleQualitaetssicherungAbgeschlossen(String ticketId) {
        List<Checklist> checklisten = checklistRepository.findByTicketId(ticketId);
        return !checklisten.isEmpty() && checklisten.stream().allMatch(checklist -> checklist.getAbgeschlossenAm() != null);
    }

    // Baut aus allen ERLEDIGTEN Tasks des Tickets einen lesbaren
    // Fließtext für den Abschnitt "Durchgeführte Schritte" zusammen -
    // eine Zeile pro Task, bestehend aus Thema und (falls vorhanden)
    // den nächsten Schritten/Kommentar. So muss ich die bereits in
    // TaskPlanner erfassten Arbeitsschritte nicht ein zweites Mal von
    // Hand eintippen.
    private String baueDurchgefuehrteSchritteText(String ticketId) {
        List<Task> erledigteTasks = taskRepository.findByTicketId(ticketId).stream()
                .filter(task -> task.getStatus() == TaskStatus.ERLEDIGT)
                .toList();

        if (erledigteTasks.isEmpty()) {
            return "";
        }

        return erledigteTasks.stream()
                .map(task -> "- " + task.getThema()
                        + (task.getNaechsteSchritte() != null && !task.getNaechsteSchritte().isBlank()
                        ? ": " + task.getNaechsteSchritte()
                        : ""))
                .collect(Collectors.joining("\n"));
    }
}