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

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void getAllTasks_returnsEmptyList_whenNoTasksExist() throws Exception {
        mockMvc.perform(get("/api/tasks").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void postTask_createsNewTask_andReturns201() throws Exception {
        String requestBody = """
                {
                    "ticketId": "ticket-1",
                    "topic": "Backup prüfen",
                    "nextSteps": "Logs checken",
                    "dueDate": "2026-10-01",
                    "status": "OPEN"
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topic").value("Backup prüfen"))
                .andExpect(jsonPath("$.recordedAt").exists());
    }

    @Test
    void postTask_returns400_whenTopicIsEmpty() throws Exception {
        String requestBody = """
                {
                    "ticketId": "ticket-1",
                    "topic": "",
                    "status": "OPEN"
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTaskById_returnsTask_whenIdExists() throws Exception {
        Task savedTask = taskRepository.save(new Task(null, "ticket-1", "Backup prüfen",
                "Logs checken", LocalDateTime.now(), LocalDate.now(), null, TaskStatus.OPEN));

        mockMvc.perform(get("/api/tasks/" + savedTask.getId()).with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("Backup prüfen"));
    }

    @Test
    void getTaskById_returns404_whenIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/tasks/unbekannt").with(oauth2Login()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void putTask_updatesTask_andSetsDoneAt() throws Exception {
        Task savedTask = taskRepository.save(new Task(null, "ticket-1", "Backup prüfen",
                "Logs checken", LocalDateTime.now(), LocalDate.now(), null, TaskStatus.IN_PROGRESS));

        String requestBody = """
                {
                    "ticketId": "ticket-1",
                    "topic": "Backup prüfen",
                    "nextSteps": "fertig",
                    "dueDate": "2026-10-01",
                    "status": "DONE"
                }
                """;

        mockMvc.perform(put("/api/tasks/" + savedTask.getId())
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.doneAt").exists());
    }

    @Test
    void deleteTask_removesTask_andReturns204() throws Exception {
        Task savedTask = taskRepository.save(new Task(null, "ticket-1", "Backup prüfen",
                "Logs checken", LocalDateTime.now(), LocalDate.now(), null, TaskStatus.OPEN));

        mockMvc.perform(delete("/api/tasks/" + savedTask.getId()).with(oauth2Login()).with(csrf()))
                .andExpect(status().isNoContent());
    }
}
