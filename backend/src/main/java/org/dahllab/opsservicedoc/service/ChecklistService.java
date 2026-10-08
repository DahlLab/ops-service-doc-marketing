package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.ChecklistDto;
import org.dahllab.opsservicedoc.dto.ChecklistItemDto;
import org.dahllab.opsservicedoc.model.Checklist;
import org.dahllab.opsservicedoc.model.ChecklistItem;
import org.dahllab.opsservicedoc.model.ChecklistTemplate;
import org.dahllab.opsservicedoc.repository.ChecklistRepository;
import org.dahllab.opsservicedoc.repository.ChecklistTemplateRepository;
import org.dahllab.opsservicedoc.util.ChecklistMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ChecklistService {

    // Gemeinsamer Teil der "nicht gefunden"-Fehlermeldungen: ein Literal statt vieler Kopien
    private static final String RESOURCE_NOT_FOUND = " nicht gefunden";

    private final ChecklistRepository checklistRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;

    public ChecklistService(ChecklistRepository checklistRepository,
                            ChecklistTemplateRepository checklistTemplateRepository) {
        this.checklistRepository = checklistRepository;
        this.checklistTemplateRepository = checklistTemplateRepository;
    }

    // GET /api/checklists - liefert alle Checklisten.
    public List<ChecklistDto> getAllChecklists() {
        return checklistRepository.findAll().stream()
                .map(ChecklistMapper::toDto)
                .toList();
    }

    // GET /api/checklists?ticketId=... - liefert nur die Checklisten
    // zu einem bestimmten Ticket.
    public List<ChecklistDto> getChecklistsByTicketId(String ticketId) {
        return checklistRepository.findByTicketId(ticketId).stream()
                .map(ChecklistMapper::toDto)
                .toList();
    }

    // GET /api/checklists/{id} - liefert genau eine Checkliste.
    public ChecklistDto getChecklistById(String id) {
        Checklist result = checklistRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Checkliste mit ID " + id + RESOURCE_NOT_FOUND));
        return ChecklistMapper.toDto(result);
    }

    // POST /api/checklists - legt eine neue Checkliste mit manuell
    // mitgeschickten Items an.
    public ChecklistDto createChecklist(ChecklistDto checklistDto) {
        List<ChecklistItem> items = createItemsWithId(checklistDto.items());

        Checklist newChecklist = new Checklist(
                null,
                checklistDto.ticketId(),
                checklistDto.title(),
                items,
                LocalDateTime.now(ZoneId.systemDefault()),
                null
        );

        setCompletionDateIfAllDone(newChecklist);

        Checklist result = checklistRepository.save(newChecklist);
        return ChecklistMapper.toDto(result);
    }

    // POST /api/checklists/from-template - legt eine neue Checkliste
    // anhand einer vorhandenen ChecklistTemplate an: der Name der
    // Vorlage wird zum Checklisten-Titel, aus den
    // Item-Beschreibungen der Vorlage erzeuge ich frische
    // ChecklistItems (jeweils mit neuer UUID, erledigt=false). Existiert
    // die Vorlage nicht, wirft es eine NoSuchElementException, die der
    // GlobalExceptionHandler in 404 übersetzt.
    public ChecklistDto createChecklistFromTemplate(String ticketId, String templateId) {
        ChecklistTemplate template = checklistTemplateRepository.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Checklisten-Vorlage mit ID " + templateId + RESOURCE_NOT_FOUND));

        List<ChecklistItem> items = template.getItemDescriptions().stream()
                .map(description -> new ChecklistItem(UUID.randomUUID().toString(), description, false))
                .toList();

        Checklist newChecklist = new Checklist(
                null,
                ticketId,
                template.getName(),
                items,
                LocalDateTime.now(ZoneId.systemDefault()),
                null
        );

        Checklist result = checklistRepository.save(newChecklist);
        return ChecklistMapper.toDto(result);
    }

    // PUT /api/checklists/{id} - aktualisiert Titel und Items einer
    // bestehenden Checkliste (z.B. um Items abzuhaken). erstelltAm
    // bleibt unverändert, abgeschlossenAm wird jedes Mal neu bewertet.
    public ChecklistDto updateChecklist(String id, ChecklistDto checklistDto) {
        Checklist existingChecklist = checklistRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Checkliste mit ID " + id + RESOURCE_NOT_FOUND));

        List<ChecklistItem> items = createItemsWithId(checklistDto.items());

        Checklist updatedChecklist = new Checklist(
                existingChecklist.getId(),
                checklistDto.ticketId(),
                checklistDto.title(),
                items,
                existingChecklist.getCreatedAt(),
                null
        );

        setCompletionDateIfAllDone(updatedChecklist);

        Checklist result = checklistRepository.save(updatedChecklist);
        return ChecklistMapper.toDto(result);
    }

    // DELETE /api/checklists/{id} - löscht eine Checkliste.
    public void deleteChecklist(String id) {
        if (!checklistRepository.existsById(id)) {
            throw new NoSuchElementException("Checkliste mit ID " + id + RESOURCE_NOT_FOUND);
        }
        checklistRepository.deleteById(id);
    }

    // Erzeugt aus den ChecklistItemDtos echte ChecklistItem-Objekte und
    // vergibt dabei eine neue UUID für jedes Item, das noch keine ID
    // hat.
    private List<ChecklistItem> createItemsWithId(List<ChecklistItemDto> itemDtos) {
        return itemDtos.stream()
                .map(itemDto -> new ChecklistItem(
                        itemDto.id() != null ? itemDto.id() : UUID.randomUUID().toString(),
                        itemDto.description(),
                        itemDto.done()
                ))
                .toList();
    }

    // Setzt abgeschlossenAm auf jetzt, wenn wirklich alle Items der
    // Checkliste erledigt sind - sonst bleibt es null.
    private void setCompletionDateIfAllDone(Checklist checklist) {
        boolean allDone = checklist.getItems().stream().allMatch(ChecklistItem::isDone);
        if (allDone) {
            checklist.setCompletedAt(LocalDateTime.now(ZoneId.systemDefault()));
        }
    }
}
