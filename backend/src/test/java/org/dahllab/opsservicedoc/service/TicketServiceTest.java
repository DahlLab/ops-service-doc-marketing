package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.TicketDto;
import org.dahllab.opsservicedoc.model.ScenarioType;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.model.TicketStatus;
import org.dahllab.opsservicedoc.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    @DisplayName("GIVEN an empty database WHEN getAllTickets is called THEN mock tickets are created and returned")
    void getAllTickets_createsMockData_whenDatabaseIsEmpty() {
        Ticket ticket1 = new Ticket("1", "GLPI-1001", "Server maintenance", "Description",
                TicketStatus.NEW, "M.Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        Ticket ticket2 = new Ticket("2", "GLPI-1002", "Backup check", "Description",
                TicketStatus.IN_PROGRESS, "N. Uhura", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        when(ticketRepository.count()).thenReturn(0L);
        when(ticketRepository.findAll()).thenReturn(List.of(ticket1, ticket2));

        List<TicketDto> result = ticketService.getAllTickets();

        assertEquals(2, result.size());
        assertEquals("Server maintenance", result.get(0).title());
        verify(ticketRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("GIVEN an already filled database WHEN getAllTickets is called THEN NO new mock data is created")
    void getAllTickets_createsNoMockData_whenDatabaseIsAlreadyFilled() {
        Ticket existingTicket = new Ticket("1", "GLPI-1001", "Server maintenance", "Description",
                TicketStatus.NEW, "M.Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        when(ticketRepository.count()).thenReturn(1L);
        when(ticketRepository.findAll()).thenReturn(List.of(existingTicket));

        List<TicketDto> result = ticketService.getAllTickets();

        assertEquals(1, result.size());
        verify(ticketRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("GIVEN an existing ID WHEN getTicketById is called THEN the matching ticket is returned")
    void getTicketById_returnsTicket_whenIdExists() {
        Ticket ticket = new Ticket("1", "GLPI-1001", "Server maintenance", "Description",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.findById("1")).thenReturn(java.util.Optional.of(ticket));

        TicketDto result = ticketService.getTicketById("1");

        assertEquals("Server maintenance", result.title());
    }

    @Test
    @DisplayName("GIVEN a non-existing ID WHEN getTicketById is called THEN a NoSuchElementException is thrown")
    void getTicketById_throwsException_whenIdDoesNotExist() {
        when(ticketRepository.findById("unknown")).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                java.util.NoSuchElementException.class,
                () -> ticketService.getTicketById("unknown")
        );
    }

    @Test
    @DisplayName("GIVEN a new ticket WHEN createTicket is called THEN it is saved and returned as DTO")
    void createTicket_savesAndReturnsTicket() {
        TicketDto newTicketDto = new TicketDto(null, "New ticket", "Description",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, null);

        Ticket savedTicket = new Ticket("1", null, "New ticket", "Description",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class))).thenReturn(savedTicket);

        TicketDto result = ticketService.createTicket(newTicketDto);

        assertEquals("1", result.id());
        assertEquals("New ticket", result.title());
    }

    @Test
    @DisplayName("GIVEN an existing ID WHEN updateTicket is called THEN the ticket is updated")
    void updateTicket_updatesTicket_whenIdExists() {
        TicketDto updatedData = new TicketDto("1", "Changed title", "Description",
                TicketStatus.SOLVED, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        when(ticketRepository.existsById("1")).thenReturn(true);

        Ticket savedTicket = new Ticket("1", null, "Changed title", "Description",
                TicketStatus.SOLVED, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class))).thenReturn(savedTicket);

        TicketDto result = ticketService.updateTicket("1", updatedData);

        assertEquals("Changed title", result.title());
        assertEquals(TicketStatus.SOLVED, result.status());
    }

    @Test
    @DisplayName("GIVEN a non-existing ID WHEN updateTicket is called THEN a NoSuchElementException is thrown")
    void updateTicket_throwsException_whenIdDoesNotExist() {
        when(ticketRepository.existsById("unknown")).thenReturn(false);

        TicketDto anyData = new TicketDto("unknown", "Title", "Description",
                TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());

        org.junit.jupiter.api.Assertions.assertThrows(
                java.util.NoSuchElementException.class,
                () -> ticketService.updateTicket("unknown", anyData)
        );
    }

    @Mock
    private GlpiClient glpiClient;

    @Test
    @DisplayName("GIVEN a new GLPI ticket WHEN syncFromGlpi is called THEN it is created")
    void syncFromGlpi_createsNewTicket_whenNotYetPresent() {
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", 2001);
        glpiTicket.put("name", "GLPI ticket");
        glpiTicket.put("status", 1);

        when(glpiClient.getAllGlpiTickets()).thenReturn(List.of(glpiTicket));

        when(ticketRepository.findByGlpiTicketId("2001")).thenReturn(java.util.Optional.empty());

        Ticket savedTicket = new Ticket("1", "2001", "GLPI ticket", "",
                TicketStatus.NEW, "Unassigned", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenReturn(savedTicket);

        List<TicketDto> result = ticketService.syncFromGlpi();

        assertEquals(1, result.size());
        assertEquals("GLPI ticket", result.get(0).title());
    }

    @Test
    @DisplayName("GIVEN an already imported GLPI ticket WHEN syncFromGlpi is called again THEN the existing ticket is updated instead of duplicated")
    void syncFromGlpi_updatesExistingTicket_whenGlpiTicketIdAlreadyExists() {
        Map<String, Object> glpiTicket = new HashMap<>();
        glpiTicket.put("id", 2001);
        glpiTicket.put("name", "Changed title");
        glpiTicket.put("status", 5);

        when(glpiClient.getAllGlpiTickets()).thenReturn(List.of(glpiTicket));

        Ticket existingTicket = new Ticket("bestehende-mongo-id", "2001", "Old title", "",
                TicketStatus.NEW, "Unassigned", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.findByGlpiTicketId("2001")).thenReturn(java.util.Optional.of(existingTicket));

        Ticket updatedTicket = new Ticket("bestehende-mongo-id", "2001", "Changed title", "",
                TicketStatus.SOLVED, "Unassigned", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now());
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenReturn(updatedTicket);

        List<TicketDto> result = ticketService.syncFromGlpi();

        assertEquals(1, result.size());
        assertEquals("Changed title", result.get(0).title());
        assertEquals(TicketStatus.SOLVED, result.get(0).status());
    }
}
