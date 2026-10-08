package org.dahllab.opsservicedoc.controller;

import org.dahllab.opsservicedoc.model.ChecklistTemplate;
import org.dahllab.opsservicedoc.repository.ChecklistTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChecklistTemplateControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChecklistTemplateRepository checklistTemplateRepository;

    @BeforeEach
    void setUp() {
        checklistTemplateRepository.deleteAll();
    }

    @Test
    void getAllTemplates_returnsEmptyList_whenNoTemplatesExist() throws Exception {
        mockMvc.perform(get("/api/checklist-templates").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void postTemplate_createsNewTemplate_andReturns201() throws Exception {
        String requestBody = """
                {
                    "name": "Server-Wartung Standard",
                    "itemDescriptions": ["USV geprüft", "Backup getestet"]
                }
                """;

        mockMvc.perform(post("/api/checklist-templates")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Server-Wartung Standard"))
                .andExpect(jsonPath("$.itemDescriptions.length()").value(2));
    }

    @Test
    void postTemplate_returns400_whenNameIsEmpty() throws Exception {
        String requestBody = """
                {
                    "name": "",
                    "itemDescriptions": ["USV geprüft"]
                }
                """;

        mockMvc.perform(post("/api/checklist-templates")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTemplateById_returnsTemplate_whenIdExists() throws Exception {
        ChecklistTemplate savedTemplate = checklistTemplateRepository.save(
                new ChecklistTemplate(null, "Server-Wartung Standard", List.of("USV geprüft"), false));

        mockMvc.perform(get("/api/checklist-templates/" + savedTemplate.getId()).with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Server-Wartung Standard"));
    }

    @Test
    void getTemplateById_returns404_whenIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/checklist-templates/unbekannt").with(oauth2Login()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void putTemplate_updatesTemplate() throws Exception {
        ChecklistTemplate savedTemplate = checklistTemplateRepository.save(
                new ChecklistTemplate(null, "Server-Wartung Standard", List.of("USV geprüft"), false));

        String requestBody = """
                {
                    "name": "Server-Wartung Erweitert",
                    "itemDescriptions": ["USV geprüft", "Backup getestet"]
                }
                """;

        mockMvc.perform(put("/api/checklist-templates/" + savedTemplate.getId())
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Server-Wartung Erweitert"));
    }

    @Test
    void deleteTemplate_removesTemplate_andReturns204() throws Exception {
        ChecklistTemplate savedTemplate = checklistTemplateRepository.save(
                new ChecklistTemplate(null, "Server-Wartung Standard", List.of("USV geprüft"), false));

        mockMvc.perform(delete("/api/checklist-templates/" + savedTemplate.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTemplate_returns409_forBuiltInTemplate() throws Exception {
        ChecklistTemplate builtInTemplate = checklistTemplateRepository.save(
                new ChecklistTemplate(null, "Server", List.of("[Vorbereitung] Backup prüfen"), true));

        mockMvc.perform(delete("/api/checklist-templates/" + builtInTemplate.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isConflict());

        assertThat(checklistTemplateRepository.existsById(builtInTemplate.getId())).isTrue();
    }

    @Test
    void getAllTemplates_returns401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/checklist-templates"))
                .andExpect(status().isUnauthorized());
    }
}
