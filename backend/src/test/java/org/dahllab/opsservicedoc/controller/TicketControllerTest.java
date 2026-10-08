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

    // Bereits von Spring Boot fertig konfigurierte Jackson-Instanz -
    // ich baue keine eigene, um Inkonsistenzen mit der echten App-Konfiguration
    // zu vermeiden (DRY: eine zentrale ObjectMapper-Konfiguration für die ganze App).
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean: ersetzt den echten TicketService im Spring-Kontext durch
    // eine Mockito-Mock. So teste ich NUR den Controller (HTTP-Handling,
    // Security-Regeln), ohne dass due echte Service-Logik/datenbank mitläuft
    // (Single Responsibility: dieser Test prüft die Schnittstelle, nicht die Logik).
    @MockitoBean
    private TicketService ticketService;

    @Test
    @DisplayName("GIVEN a logged-in user WHEN GET /api/tickets is called THEN the tickets are returned as JSON")
    void getAllTickets_returnsTickets_whenLoggedIn() throws Exception {

        // GIVEN: Ich bereite vor, was der (gemockte) Service zurückgeben soll,
        // wenn seine Methode aufgerufen wird, unabhängig von der echten Logik.
        TicketDto ticketDto = new TicketDto(
                "1", "Server-Wartung", "Beschreibung",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now()
        );
        when(ticketService.getAllTickets()).thenReturn(List.of(ticketDto));

        // WHEN: Simulierter, eingeloggter GET-Request auf /api/tickets.
        var result = mockMvc.perform(
                get("/api/tickets").with(oauth2Login())
        );

        // THEN: Status 200 UND der Titel des ersten Tickets im JSON-Array muss stimmen
        // (jsonPAth prüft gezielt einen Wert innerhalb der JSON-Antwort).
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Server-Wartung"));
    }

    @Test
    @DisplayName("GIVEN no logged-in user WHEN GET /api/tickets is called THEN 401 Unauthorized is returned")
    void getAllTickets_returns401_whenNotLoggedIn() throws Exception {

        // WHEN: Request OHNE simulierten Login.
        var result = mockMvc.perform(get("/api/tickets"));

        // THEN: Die SecurityConfig schützt /api/tickets/**,  ohne Login muss 401 kommen.
        result.andExpect(status().isUnauthorized());
    }



    @Test
    @DisplayName("GIVEN a logged-in user WHEN GET /api/tickets/{id} is called THEN the ticket is returned")
    void getTicketById_returnsTicket_whenLoggedIn() throws Exception {

        // GIVEN:
        TicketDto ticketDto = new TicketDto("1", "Server-Wartung", "Beschreibung",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketService.getTicketById("1")).thenReturn(ticketDto);

        // WHEN:
        var result = mockMvc.perform(
                get("/api/tickets/1").with(oauth2Login())
        );

        // THEN:
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Server-Wartung"));
    }

    @Test
    @DisplayName("GIVEN a logged-in user WHEN POST /api/tickets is called with valid data THEN the ticket is created")
    void createTicket_createsTicket_whenDataIsValid() throws Exception {

        // GIVEN:
        TicketDto newTicket = new TicketDto(null, "Neues Ticket", "Beschreibung",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        TicketDto savedTicket = new TicketDto("1", "Neues Ticket", "Beschreibung",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        when(ticketService.createTicket(org.mockito.ArgumentMatchers.any(TicketDto.class)))
                .thenReturn(savedTicket);

        // WHEN:
        // objectMapper ist die von Spring Boot bereits fertig konfigurierte
        // Bean (siehe @Autowired-Feld oben in der Klasse) - wandelt unser
        // TicketDto-Objekt in einen JSON-String für den Request-Body um.
        var result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tickets")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTicket))
        );

        // THEN:
        // 201 Created, wie im Controller mit @ResponseStatus(HttpStatus.CREATED) festgelegt.
        result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    @DisplayName("GIVEN a logged-in user WHEN PUT /api/tickets/{id} is called THEN the ticket is updated")
    void updateTicket_updatesTicket_whenLoggedIn() throws Exception {

        // GIVEN:
        TicketDto updatedTicket = new TicketDto("1", "Geänderter Titel", "Beschreibung",
                TicketStatus.SOLVED, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        when(ticketService.updateTicket(org.mockito.ArgumentMatchers.eq("1"), org.mockito.ArgumentMatchers.any(TicketDto.class)))
                .thenReturn(updatedTicket);

        // WHEN:
        var result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/tickets/1")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedTicket))
        );

        // THEN:
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Geänderter Titel"));
    }
}
