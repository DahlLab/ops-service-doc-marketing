package org.dahllab.opsservicedoc.controller;

import org.dahllab.opsservicedoc.dto.TicketDto;
import org.dahllab.opsservicedoc.model.ScenarioType;
import org.dahllab.opsservicedoc.model.TicketStatus;
import org.dahllab.opsservicedoc.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    @DisplayName("GIVEN a logged-in user WHEN GET /api/tickets is called THEN the tickets are returned as JSON")
    void getAllTickets_returnsTickets_whenLoggedIn() throws Exception {
        TicketDto ticketDto = new TicketDto(
                "1", "Server maintenance", "Description",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now()
        );
        when(ticketService.getAllTickets()).thenReturn(List.of(ticketDto));

        var result = mockMvc.perform(
                get("/api/tickets").with(oauth2Login())
        );

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Server maintenance"));
    }

    @Test
    @DisplayName("GIVEN no logged-in user WHEN GET /api/tickets is called THEN 401 Unauthorized is returned")
    void getAllTickets_returns401_whenNotLoggedIn() throws Exception {
        var result = mockMvc.perform(get("/api/tickets"));

        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GIVEN a logged-in user WHEN GET /api/tickets/{id} is called THEN the ticket is returned")
    void getTicketById_returnsTicket_whenLoggedIn() throws Exception {
        TicketDto ticketDto = new TicketDto("1", "Server maintenance", "Description",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketService.getTicketById("1")).thenReturn(ticketDto);

        var result = mockMvc.perform(
                get("/api/tickets/1").with(oauth2Login())
        );

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Server maintenance"));
    }

    @Test
    @DisplayName("GIVEN a logged-in user WHEN POST /api/tickets is called with valid data THEN the ticket is created")
    void createTicket_createsTicket_whenDataIsValid() throws Exception {
        TicketDto newTicket = new TicketDto(null, "New ticket", "Description",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        TicketDto savedTicket = new TicketDto("1", "New ticket", "Description",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        when(ticketService.createTicket(org.mockito.ArgumentMatchers.any(TicketDto.class)))
                .thenReturn(savedTicket);

        var result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tickets")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTicket))
        );

        result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    @DisplayName("GIVEN a logged-in user WHEN PUT /api/tickets/{id} is called THEN the ticket is updated")
    void updateTicket_updatesTicket_whenLoggedIn() throws Exception {
        TicketDto updatedTicket = new TicketDto("1", "Changed title", "Description",
                TicketStatus.SOLVED, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        when(ticketService.updateTicket(org.mockito.ArgumentMatchers.eq("1"), org.mockito.ArgumentMatchers.any(TicketDto.class)))
                .thenReturn(updatedTicket);

        var result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/tickets/1")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedTicket))
        );

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Changed title"));
    }
}
