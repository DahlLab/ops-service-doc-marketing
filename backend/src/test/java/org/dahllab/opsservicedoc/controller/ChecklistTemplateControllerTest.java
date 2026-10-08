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

// @SpringBootTest + @AutoConfigureMockMvc: startet den kompletten
// Anwendungskontext und stellt MockMvc bereit, damit ich echte
// HTTP-Anfragen gegen den Controller simulieren kann, inklusive
// Security (oauth2Login()) und echter MongoDB-Anbindung - gleiches
// Muster wie ChecklistControllerTest.
@SpringBootTest
@AutoConfigureMockMvc
class ChecklistTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChecklistTemplateRepository checklistTemplateRepository;

    // Ich leere die ChecklistTemplate-Collection vor jedem Test, damit
    // die Tests unabhängig voneinander laufen.
    @BeforeEach
    void setUp() {
        checklistTemplateRepository.deleteAll();
    }

    @Test
    void getAllTemplates_gibtLeereListeZurueck_wennKeineVorlagenVorhanden() throws Exception {
        mockMvc.perform(get("/api/checklist-templates").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Prüft das Anlegen einer Vorlage über POST: erwarte 201 sowie die
    // mitgeschickten Werte in der Antwort.
    @Test
    void postTemplate_legtNeueVorlageAn_undGibt201Zurueck() throws Exception {
        String requestBody = """
                {
                    "name": "Server-Wartung Standard",
                    "itemBeschreibungen": ["USV geprüft", "Backup getestet"]
                }
                """;

        mockMvc.perform(post("/api/checklist-templates")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Server-Wartung Standard"))
                .andExpect(jsonPath("$.itemBeschreibungen.length()").value(2));
    }

    // Prüft die Validierung: ein leerer Name muss mit 400 abgelehnt
    // werden.
    @Test
    void postTemplate_gibt400Zurueck_wennNameLeerIst() throws Exception {
        String requestBody = """
                {
                    "name": "",
                    "itemBeschreibungen": ["USV geprüft"]
                }
                """;

        mockMvc.perform(post("/api/checklist-templates")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // Prüft den Erfolgsfall von GET /api/checklist-templates/{id}: ich
    // lege die Vorlage vorher direkt über das Repository an, um den
    // Controller unabhängig vom POST-Endpunkt zu testen.
    @Test
    void getTemplateById_gibtVorlageZurueck_wennIdExistiert() throws Exception {
        ChecklistTemplate gespeicherteTemplate = checklistTemplateRepository.save(
                new ChecklistTemplate(null, "Server-Wartung Standard", List.of("USV geprüft"), false));

        mockMvc.perform(get("/api/checklist-templates/" + gespeicherteTemplate.getId()).with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Server-Wartung Standard"));
    }

    // Prüft den Fehlerfall: eine unbekannte ID muss zu einem
    // 4xx-Fehler führen.
    @Test
    void getTemplateById_gibt404_wennIdNichtExistiert() throws Exception {
        mockMvc.perform(get("/api/checklist-templates/unbekannt").with(oauth2Login()))
                .andExpect(status().is4xxClientError());
    }

    // Prüft PUT /api/checklist-templates/{id}: die Vorlage muss mit
    // den neuen Werten aktualisiert werden.
    @Test
    void putTemplate_aktualisiertVorlage() throws Exception {
        ChecklistTemplate gespeicherteTemplate = checklistTemplateRepository.save(
                new ChecklistTemplate(null, "Server-Wartung Standard", List.of("USV geprüft"), false));

        String requestBody = """
                {
                    "name": "Server-Wartung Erweitert",
                    "itemBeschreibungen": ["USV geprüft", "Backup getestet"]
                }
                """;

        mockMvc.perform(put("/api/checklist-templates/" + gespeicherteTemplate.getId())
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Server-Wartung Erweitert"));
    }

    // Prüft DELETE /api/checklist-templates/{id}: erfolgreiches
    // Löschen muss 204 No Content liefern.
    @Test
    void deleteTemplate_entferntVorlage_undGibt204Zurueck() throws Exception {
        ChecklistTemplate gespeicherteTemplate = checklistTemplateRepository.save(
                new ChecklistTemplate(null, "Server-Wartung Standard", List.of("USV geprüft"), false));

        mockMvc.perform(delete("/api/checklist-templates/" + gespeicherteTemplate.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    // Prüft den Löschschutz: eine Standard-Vorlage (standard=true, wie sie
    // der ChecklistTemplateSeeder anlegt) darf nicht gelöscht werden - der
    // GlobalExceptionHandler muss die IllegalStateException aus dem Service
    // als 409 Conflict ausliefern, und die Vorlage muss danach noch
    // existieren.
    @Test
    void deleteTemplate_gibt409_beiStandardVorlage() throws Exception {
        ChecklistTemplate standardTemplate = checklistTemplateRepository.save(
                new ChecklistTemplate(null, "Server", List.of("[Vorbereitung] Backup prüfen"), true));

        mockMvc.perform(delete("/api/checklist-templates/" + standardTemplate.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isConflict());

        assertThat(checklistTemplateRepository.existsById(standardTemplate.getId())).isTrue();
    }

    // Prüft, dass der Endpunkt ohne Login geschützt ist - die SecurityConfig
    // sichert alle Pfade unter /api/ ab. Ohne diesen Test würde es nicht
    // auffallen, wenn ein neuer Controller versehentlich offen bliebe.
    @Test
    void getAllTemplates_gibt401_wennNichtEingeloggt() throws Exception {
        mockMvc.perform(get("/api/checklist-templates"))
                .andExpect(status().isUnauthorized());
    }
}
