package org.dahllab.opsservicedoc.controller;

import org.dahllab.opsservicedoc.model.Task;
import org.dahllab.opsservicedoc.model.TaskStatus;
import org.dahllab.opsservicedoc.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest + @AutoConfigureMockMvc: startet den kompletten
// Anwendungskontext und stellt MockMvc bereit, damit ich echte
// HTTP-Anfragen gegen den Controller simulieren kann, inklusive
// Security (oauth2Login()) und echter MongoDB-Anbindung.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Ich leere die Task-Collection vor jedem Test, damit die Tests
    // unabhängig voneinander laufen und sich nicht gegenseitig
    // beeinflussen (keine Reihenfolge-Abhängigkeit zwischen den Tests).
    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    // Prüfe den Fall, dass noch kein Task angelegt wurde: der Endpunkt
    // muss dann eine leere Liste liefern, keinen Fehler.
    @Test
    void getAllTasks_gibtLeereListeZurueck_wennKeineTasksVorhanden() throws Exception {
        mockMvc.perform(get("/api/tasks").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Prüfe das Anlegen eines neuen Tasks über POST: erwarte Status 201
    // (Created) sowie die im Body mitgeschickten Werte in der Antwort.
    // erfasstAm wird vom Service automatisch gesetzt - das prüfe ich
    // mit, damit ich sicher bin, dass diese Automatik auch über den
    // echten HTTP-Weg funktioniert und nicht nur im Unit-Test.
    @Test
    void postTask_legtNeuenTaskAn_undGibt201Zurueck() throws Exception {
        String requestBody = """
                {
                    "ticketId": "ticket-1",
                    "thema": "Backup prüfen",
                    "naechsteSchritte": "Logs checken",
                    "zieldatum": "2026-10-01",
                    "status": "OFFEN"
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thema").value("Backup prüfen"))
                .andExpect(jsonPath("$.erfasstAm").exists());
    }

    // Prüfe die Validierung: ein leeres thema muss vom Controller mit
    // 400 Bad Request abgelehnt werden, bevor überhaupt ein Task
    // angelegt wird (greift über @NotBlank im TaskDto + @Valid im
    // Controller).
    @Test
    void postTask_gibt400Zurueck_wennThemaLeerIst() throws Exception {
        String requestBody = """
                {
                    "ticketId": "ticket-1",
                    "thema": "",
                    "status": "OFFEN"
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // Prüfe den Erfolgsfall von GET /api/tasks/{id}: ich lege vorher
    // direkt über das Repository einen Task an, um den Controller
    // unabhängig vom POST-Endpunkt zu testen.
    @Test
    void getTaskById_gibtTaskZurueck_wennIdExistiert() throws Exception {
        Task gespeicherterTask = taskRepository.save(new Task(null, "ticket-1", "Backup prüfen",
                "Logs checken", LocalDateTime.now(), LocalDate.now(), null, TaskStatus.OFFEN));

        mockMvc.perform(get("/api/tasks/" + gespeicherterTask.getId()).with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thema").value("Backup prüfen"));
    }

    // Prüfe den Fehlerfall von GET /api/tasks/{id}: eine unbekannte ID
    // muss zu einem 4xx-Fehler führen (die NoSuchElementException aus
    // dem Service).
    @Test
    void getTaskById_gibt404_wennIdNichtExistiert() throws Exception {
        mockMvc.perform(get("/api/tasks/unbekannt").with(oauth2Login()))
                .andExpect(status().is4xxClientError());
    }

    // Prüfe PUT /api/tasks/{id} zusammen mit der automatischen
    // erledigtAm-Logik: wechsle ich den Status eines bestehenden Tasks
    // auf ERLEDIGT, muss die Antwort ein gesetztes erledigtAm enthalten,
    // ohne dass ich es im Request-Body mitschicken muss.
    @Test
    void putTask_aktualisiertTask_undSetztErledigtAm() throws Exception {
        Task gespeicherterTask = taskRepository.save(new Task(null, "ticket-1", "Backup prüfen",
                "Logs checken", LocalDateTime.now(), LocalDate.now(), null, TaskStatus.IN_BEARBEITUNG));

        String requestBody = """
                {
                    "ticketId": "ticket-1",
                    "thema": "Backup prüfen",
                    "naechsteSchritte": "fertig",
                    "zieldatum": "2026-10-01",
                    "status": "ERLEDIGT"
                }
                """;

        mockMvc.perform(put("/api/tasks/" + gespeicherterTask.getId())
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERLEDIGT"))
                .andExpect(jsonPath("$.erledigtAm").exists());
    }

    // Prüfe DELETE /api/tasks/{id}: erfolgreiches Löschen muss 204 No
    // Content liefern (kein Response-Body).
    @Test
    void deleteTask_entferntTask_undGibt204Zurueck() throws Exception {
        Task gespeicherterTask = taskRepository.save(new Task(null, "ticket-1", "Backup prüfen",
                "Logs checken", LocalDateTime.now(), LocalDate.now(), null, TaskStatus.OFFEN));

        mockMvc.perform(delete("/api/tasks/" + gespeicherterTask.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isNoContent());
    }
}