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
        ChecklistTemplate template = new ChecklistTemplate("template-1", "Server Maintenance Standard",
                List.of("UPS checked"), false);
        when(checklistTemplateRepository.findAll()).thenReturn(List.of(template));

        List<ChecklistTemplateDto> result = checklistTemplateService.getAllTemplates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Server Maintenance Standard");
    }

    @Test
    void getTemplateById_returnsTemplate_whenIdExists() {
        ChecklistTemplate template = new ChecklistTemplate("template-1", "Server Maintenance Standard",
                List.of("UPS checked"), false);
        when(checklistTemplateRepository.findById("template-1")).thenReturn(Optional.of(template));

        ChecklistTemplateDto result = checklistTemplateService.getTemplateById("template-1");

        assertThat(result.name()).isEqualTo("Server Maintenance Standard");
    }

    @Test
    void getTemplateById_throwsException_whenIdDoesNotExist() {
        when(checklistTemplateRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checklistTemplateService.getTemplateById("unknown"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createTemplate_createsNewTemplate() {
        ChecklistTemplateDto templateDto = new ChecklistTemplateDto(null, "Server Maintenance Standard",
                List.of("UPS checked"), false);
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistTemplateDto result = checklistTemplateService.createTemplate(templateDto);

        assertThat(result.name()).isEqualTo("Server Maintenance Standard");
        assertThat(result.itemDescriptions()).containsExactly("UPS checked");
    }

    @Test
    void updateTemplate_updatesExistingTemplate() {
        ChecklistTemplate existingTemplate = new ChecklistTemplate("template-1", "Server Maintenance Standard",
                List.of("UPS checked"), false);
        when(checklistTemplateRepository.findById("template-1")).thenReturn(Optional.of(existingTemplate));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistTemplateDto templateDto = new ChecklistTemplateDto(null, "Server Maintenance Extended",
                List.of("UPS checked", "Backup tested"), false);

        ChecklistTemplateDto result = checklistTemplateService.updateTemplate("template-1", templateDto);

        assertThat(result.name()).isEqualTo("Server Maintenance Extended");
        assertThat(result.itemDescriptions()).hasSize(2);
    }

    @Test
    void updateTemplate_throwsException_whenIdDoesNotExist() {
        when(checklistTemplateRepository.findById("unknown")).thenReturn(Optional.empty());
        ChecklistTemplateDto templateDto = new ChecklistTemplateDto(null, "Server Maintenance Standard",
                List.of("UPS checked"), false);

        assertThatThrownBy(() -> checklistTemplateService.updateTemplate("unknown", templateDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteTemplate_deletesTemplate_whenIdExists() {
        ChecklistTemplate template = new ChecklistTemplate("template-1", "Custom template",
                List.of("UPS checked"), false);
        when(checklistTemplateRepository.findById("template-1")).thenReturn(Optional.of(template));

        checklistTemplateService.deleteTemplate("template-1");

        verify(checklistTemplateRepository).deleteById("template-1");
    }

    @Test
    void deleteTemplate_throwsException_whenIdDoesNotExist() {
        when(checklistTemplateRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checklistTemplateService.deleteTemplate("unknown"))
                .isInstanceOf(NoSuchElementException.class);

        verify(checklistTemplateRepository, never()).deleteById(any());
    }

    @Test
    void deleteTemplate_throwsException_forBuiltInTemplate() {
        ChecklistTemplate builtInTemplate = new ChecklistTemplate("template-1", "Server",
                List.of("UPS checked"), true);
        when(checklistTemplateRepository.findById("template-1")).thenReturn(Optional.of(builtInTemplate));

        assertThatThrownBy(() -> checklistTemplateService.deleteTemplate("template-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Server");

        verify(checklistTemplateRepository, never()).deleteById(any());
    }

    @Test
    void createTemplate_ignoresBuiltInFlagFromClient() {
        ChecklistTemplateDto templateDto = new ChecklistTemplateDto(null, "Custom template",
                List.of("UPS checked"), true);
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistTemplateDto result = checklistTemplateService.createTemplate(templateDto);

        assertThat(result.builtIn()).isFalse();
    }

    @Test
    void updateTemplate_keepsBuiltInStatusOfExistingTemplate() {
        ChecklistTemplate builtInTemplate = new ChecklistTemplate("template-1", "Server",
                List.of("UPS checked"), true);
        when(checklistTemplateRepository.findById("template-1")).thenReturn(Optional.of(builtInTemplate));
        when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ChecklistTemplateDto templateDto = new ChecklistTemplateDto(null, "Server customized",
                List.of("UPS checked"), false);

        ChecklistTemplateDto result = checklistTemplateService.updateTemplate("template-1", templateDto);

        assertThat(result.builtIn()).isTrue();
    }
}
