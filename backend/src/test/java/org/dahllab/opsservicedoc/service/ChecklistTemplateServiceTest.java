package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.ChecklistTemplateDto;
import org.dahllab.opsservicedoc.model.ChecklistTemplate;
import org.dahllab.opsservicedoc.repository.ChecklistTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class ChecklistTemplateServiceTest {
    @Mock
    private ChecklistTemplateRepository checklistTemplateRepository;

    @InjectMocks
    private ChecklistTemplateService checklistTemplateService;

    @Test
    void getAllTemplates_returnsAllTemplates() {
        ChecklistTemplate template = new ChecklistTemplate("template-1", "Server-Wartung Standard",
                List.of("USV geprüft"), false);
        when(checklistTemplateRepository.findAll()).thenReturn(List.of(template));

        List<ChecklistTemplateDto> result = checklistTemplateService.getAllTemplates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Server-Wartung Standard");
    }

    @Test
    void getTemplateById_returnsTemplate_whenIdExists() {
        ChecklistTemplate template = new ChecklistTemplate("template-1", "Server-Wartung Standard",
                List.of("USV geprüft"), false);
        when(checklistTemplateRepository.findById("template-1")).thenReturn(Optional.of(template));

        ChecklistTemplateDto result = checklistTemplateService.getTemplateById("template-1");

        assertThat(result.name()).isEqualTo("Server-Wartung Standard");
    }

    @Test
    void getTemplateById_throwsException_whenIdDoesNotExist() {
        when(checklistTemplateRepository.findById("unbekannt")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checklistTemplateService.getTemplateById("unbekannt"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createTemplate_createsNewTemplate() {
        ChecklistTemplateDto templateDto = new ChecklistTemplateDto(null, "Server-Wartung Standard",
                List.of("USV geprüft"), false);
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistTemplateDto result = checklistTemplateService.createTemplate(templateDto);

        assertThat(result.name()).isEqualTo("Server-Wartung Standard");
        assertThat(result.itemDescriptions()).containsExactly("USV geprüft");
    }

    @Test
    void updateTemplate_updatesExistingTemplate() {
        ChecklistTemplate existingTemplate = new ChecklistTemplate("template-1", "Server-Wartung Standard",
                List.of("USV geprüft"), false);
        when(checklistTemplateRepository.findById("template-1")).thenReturn(Optional.of(existingTemplate));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistTemplateDto templateDto = new ChecklistTemplateDto(null, "Server-Wartung Erweitert",
                List.of("USV geprüft", "Backup getestet"), false);

        ChecklistTemplateDto result = checklistTemplateService.updateTemplate("template-1", templateDto);

        assertThat(result.name()).isEqualTo("Server-Wartung Erweitert");
        assertThat(result.itemDescriptions()).hasSize(2);
    }

    @Test
    void updateTemplate_throwsException_whenIdDoesNotExist() {
        when(checklistTemplateRepository.findById("unbekannt")).thenReturn(Optional.empty());
        ChecklistTemplateDto templateDto = new ChecklistTemplateDto(null, "Server-Wartung Standard",
                List.of("USV geprüft"), false);

        assertThatThrownBy(() -> checklistTemplateService.updateTemplate("unbekannt", templateDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteTemplate_deletesTemplate_whenIdExists() {
        ChecklistTemplate template = new ChecklistTemplate("template-1", "Eigene Vorlage",
                List.of("USV geprüft"), false);
        when(checklistTemplateRepository.findById("template-1")).thenReturn(Optional.of(template));

        checklistTemplateService.deleteTemplate("template-1");

        verify(checklistTemplateRepository).deleteById("template-1");
    }

    @Test
    void deleteTemplate_throwsException_whenIdDoesNotExist() {
        when(checklistTemplateRepository.findById("unbekannt")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checklistTemplateService.deleteTemplate("unbekannt"))
                .isInstanceOf(NoSuchElementException.class);

        verify(checklistTemplateRepository, never()).deleteById(any());
    }

    @Test
    void deleteTemplate_throwsException_forBuiltInTemplate() {
        ChecklistTemplate builtInTemplate = new ChecklistTemplate("template-1", "Server",
                List.of("USV geprüft"), true);
        when(checklistTemplateRepository.findById("template-1")).thenReturn(Optional.of(builtInTemplate));

        assertThatThrownBy(() -> checklistTemplateService.deleteTemplate("template-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Server");

        verify(checklistTemplateRepository, never()).deleteById(any());
    }

    @Test
    void createTemplate_ignoresBuiltInFlagFromClient() {
        ChecklistTemplateDto templateDto = new ChecklistTemplateDto(null, "Eigene Vorlage",
                List.of("USV geprüft"), true);
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistTemplateDto result = checklistTemplateService.createTemplate(templateDto);

        assertThat(result.builtIn()).isFalse();
    }

    @Test
    void updateTemplate_keepsBuiltInStatusOfExistingTemplate() {
        ChecklistTemplate builtInTemplate = new ChecklistTemplate("template-1", "Server",
                List.of("USV geprüft"), true);
        when(checklistTemplateRepository.findById("template-1")).thenReturn(Optional.of(builtInTemplate));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ChecklistTemplateDto templateDto = new ChecklistTemplateDto(null, "Server angepasst",
                List.of("USV geprüft"), false);

        ChecklistTemplateDto result = checklistTemplateService.updateTemplate("template-1", templateDto);

        assertThat(result.builtIn()).isTrue();
    }
}
