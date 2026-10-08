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
    private static final String RESOURCE_NOT_FOUND = " not found";

    private final ChecklistRepository checklistRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;

    public ChecklistService(ChecklistRepository checklistRepository,
                            ChecklistTemplateRepository checklistTemplateRepository) {
        this.checklistRepository = checklistRepository;
        this.checklistTemplateRepository = checklistTemplateRepository;
    }

    public List<ChecklistDto> getAllChecklists() {
        return checklistRepository.findAll().stream()
                .map(ChecklistMapper::toDto)
                .toList();
    }

    public List<ChecklistDto> getChecklistsByTicketId(String ticketId) {
        return checklistRepository.findByTicketId(ticketId).stream()
                .map(ChecklistMapper::toDto)
                .toList();
    }

    public ChecklistDto getChecklistById(String id) {
        Checklist result = checklistRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Checklist with ID " + id + RESOURCE_NOT_FOUND));
        return ChecklistMapper.toDto(result);
    }

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

    public ChecklistDto createChecklistFromTemplate(String ticketId, String templateId) {
        ChecklistTemplate template = checklistTemplateRepository.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Checklist template with ID " + templateId + RESOURCE_NOT_FOUND));

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

    public ChecklistDto updateChecklist(String id, ChecklistDto checklistDto) {
        Checklist existingChecklist = checklistRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Checklist with ID " + id + RESOURCE_NOT_FOUND));

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

    public void deleteChecklist(String id) {
        if (!checklistRepository.existsById(id)) {
            throw new NoSuchElementException("Checklist with ID " + id + RESOURCE_NOT_FOUND);
        }
        checklistRepository.deleteById(id);
    }

    private List<ChecklistItem> createItemsWithId(List<ChecklistItemDto> itemDtos) {
        return itemDtos.stream()
                .map(itemDto -> new ChecklistItem(
                        itemDto.id() != null ? itemDto.id() : UUID.randomUUID().toString(),
                        itemDto.description(),
                        itemDto.done()
                ))
                .toList();
    }

    private void setCompletionDateIfAllDone(Checklist checklist) {
        boolean allDone = checklist.getItems().stream().allMatch(ChecklistItem::isDone);
        if (allDone) {
            checklist.setCompletedAt(LocalDateTime.now(ZoneId.systemDefault()));
        }
    }
}
