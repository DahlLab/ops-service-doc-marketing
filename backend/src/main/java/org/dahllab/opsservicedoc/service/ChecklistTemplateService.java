package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.ChecklistTemplateDto;
import org.dahllab.opsservicedoc.model.ChecklistTemplate;
import org.dahllab.opsservicedoc.repository.ChecklistTemplateRepository;
import org.dahllab.opsservicedoc.util.ChecklistTemplateMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ChecklistTemplateService {
    private static final String RESOURCE_NOT_FOUND = " not found";
    private static final String TEMPLATE_WITH_ID = "Checklist template with ID ";

    private final ChecklistTemplateRepository checklistTemplateRepository;

    public ChecklistTemplateService(ChecklistTemplateRepository checklistTemplateRepository) {
        this.checklistTemplateRepository = checklistTemplateRepository;
    }

    public List<ChecklistTemplateDto> getAllTemplates() {
        return checklistTemplateRepository.findAll().stream()
                .map(ChecklistTemplateMapper::toDto)
                .toList();
    }

    public ChecklistTemplateDto getTemplateById(String id) {
        ChecklistTemplate result = checklistTemplateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(TEMPLATE_WITH_ID + id + RESOURCE_NOT_FOUND));
        return ChecklistTemplateMapper.toDto(result);
    }

    public ChecklistTemplateDto createTemplate(ChecklistTemplateDto templateDto) {
        ChecklistTemplate newTemplate = new ChecklistTemplate(
                null, templateDto.name(), templateDto.itemDescriptions(), false);
        ChecklistTemplate result = checklistTemplateRepository.save(newTemplate);
        return ChecklistTemplateMapper.toDto(result);
    }

    public ChecklistTemplateDto updateTemplate(String id, ChecklistTemplateDto templateDto) {
        ChecklistTemplate existingTemplate = checklistTemplateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(TEMPLATE_WITH_ID + id + RESOURCE_NOT_FOUND));

        ChecklistTemplate updatedTemplate = new ChecklistTemplate(
                existingTemplate.getId(),
                templateDto.name(),
                templateDto.itemDescriptions(),
                existingTemplate.isBuiltIn()
        );

        ChecklistTemplate result = checklistTemplateRepository.save(updatedTemplate);
        return ChecklistTemplateMapper.toDto(result);
    }

    public void deleteTemplate(String id) {
        ChecklistTemplate template = checklistTemplateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(TEMPLATE_WITH_ID + id + RESOURCE_NOT_FOUND));

        if (template.isBuiltIn()) {
            throw new IllegalStateException(
                    "The default template \"" + template.getName() + "\" cannot be deleted");
        }

        checklistTemplateRepository.deleteById(id);
    }
}
