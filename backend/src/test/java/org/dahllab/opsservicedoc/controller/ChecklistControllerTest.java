package org.dahllab.opsservicedoc.controller;

import org.dahllab.opsservicedoc.model.Checklist;
import org.dahllab.opsservicedoc.model.ChecklistItem;
import org.dahllab.opsservicedoc.repository.ChecklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest + @AutoConfigureMockMvc: startet den kompletten
// Anwendungskontext und stellt MockMvc bereit, damit ich echte
// HTTP-Anfragen gegen den Controller simulieren kann, inklusive
// Security (oauth2Login()) und echter MongoDB-Anbindung - gleiches
// Muster wie TaskControllerTest.
@SpringBootTest
@AutoConfigureMockMvc
class ChecklistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChecklistRepository checklistRepository;

    // Ich leere die Checklist-Collection vor jedem Test, damit die
    // Tests unabhängig voneinander laufen.
    @BeforeEach
    void setUp() {
        checklistRepository.deleteAll();
    }

    @Test
    void getAllChecklists_returnsEmptyList_whenNoChecklistsExist() throws Exception {
        mockMvc.perform(get("/api/checklists").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Prüft das Anlegen einer Checkliste über POST: erwarte 201, die
    // mitgeschickten Werte in der Antwort, sowie automatisch gesetztes
    // erstelltAm und eine automatisch vergebene Item-ID.
    @Test
    void postChecklist_createsNewChecklist_andReturns201() throws Exception {
        String requestBody = """
                {
                    "ticketId": "ticket-1",
                    "title": "Server-Wartung",
                    "items": [
                        { "description": "USV geprüft", "done": false }
                    ]
                }
                """;

        mockMvc.perform(post("/api/checklists")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Server-Wartung"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.items[0].id").exists());
    }

    // Prüft die Validierung: ein leerer Titel muss mit 400 abgelehnt
    // werden.
    @Test
    void postChecklist_returns400_whenTitleIsEmpty() throws Exception {
        String requestBody = """
                {
                    "ticketId": "ticket-1",
                    "title": "",
                    "items": [
                        { "description": "USV geprüft", "done": false }
                    ]
                }
                """;

        mockMvc.perform(post("/api/checklists")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // Prüft die Validierung: eine Checkliste ohne Items muss mit 400
    // abgelehnt werden.
    @Test
    void postChecklist_returns400_whenItemsAreEmpty() throws Exception {
        String requestBody = """
                {
                    "ticketId": "ticket-1",
                    "title": "Server-Wartung",
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/checklists")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // Prüft den Erfolgsfall von GET /api/checklists/{id}: ich lege die
    // Checkliste vorher direkt über das Repository an, um den
    // Controller unabhängig vom POST-Endpunkt zu testen.
    @Test
    void getChecklistById_returnsChecklist_whenIdExists() throws Exception {
        ChecklistItem item = new ChecklistItem("item-1", "USV geprüft", false);
        Checklist savedChecklist = checklistRepository.save(new Checklist(null, "ticket-1",
                "Server-Wartung", List.of(item), LocalDateTime.now(), null));

        mockMvc.perform(get("/api/checklists/" + savedChecklist.getId()).with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Server-Wartung"));
    }

    // Prüft den Fehlerfall: eine unbekannte ID muss zu einem
    // 4xx-Fehler führen (die NoSuchElementException aus dem Service,
    // abgefangen vom GlobalExceptionHandler).
    @Test
    void getChecklistById_returns404_whenIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/checklists/unbekannt").with(oauth2Login()))
                .andExpect(status().is4xxClientError());
    }

    // Prüft PUT /api/checklists/{id} zusammen mit der automatischen
    // abgeschlossenAm-Logik: hake ich das einzige Item ab, muss die
    // Antwort ein gesetztes abgeschlossenAm enthalten.
    @Test
    void putChecklist_updatesItemsAndSetsCompletedAt() throws Exception {
        ChecklistItem item = new ChecklistItem("item-1", "USV geprüft", false);
        Checklist savedChecklist = checklistRepository.save(new Checklist(null, "ticket-1",
                "Server-Wartung", List.of(item), LocalDateTime.now(), null));

        String requestBody = """
                {
                    "ticketId": "ticket-1",
                    "title": "Server-Wartung",
                    "items": [
                        { "id": "item-1", "description": "USV geprüft", "done": true }
                    ]
                }
                """;

        mockMvc.perform(put("/api/checklists/" + savedChecklist.getId())
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].done").value(true))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    // Prüft DELETE /api/checklists/{id}: erfolgreiches Löschen muss
    // 204 No Content liefern.
    @Test
    void deleteChecklist_removesChecklist_andReturns204() throws Exception {
        ChecklistItem item = new ChecklistItem("item-1", "USV geprüft", false);
        Checklist savedChecklist = checklistRepository.save(new Checklist(null, "ticket-1",
                "Server-Wartung", List.of(item), LocalDateTime.now(), null));

        mockMvc.perform(delete("/api/checklists/" + savedChecklist.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    // Prüft, dass der Endpunkt ohne Login geschützt ist - die SecurityConfig
    // sichert alle Pfade unter /api/ ab. Ohne diesen Test würde es nicht
    // auffallen, wenn ein neuer Controller versehentlich offen bliebe.
    @Test
    void getAllChecklists_returns401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/checklists"))
                .andExpect(status().isUnauthorized());
    }
}
