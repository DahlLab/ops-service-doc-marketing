package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.TicketDto;
import org.dahllab.opsservicedoc.model.ScenarioType;
import org.dahllab.opsservicedoc.model.Ticket;
import org.dahllab.opsservicedoc.model.TicketStatus;
import org.dahllab.opsservicedoc.repository.TicketRepository;
import org.dahllab.opsservicedoc.util.GlpiTicketMapper;
import org.dahllab.opsservicedoc.util.TicketMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;

    private final GlpiClient glpiClient;

    public TicketService(TicketRepository ticketRepository, GlpiClient glpiClient) {
        this.ticketRepository = ticketRepository;
        this.glpiClient = glpiClient;
    }

    public List<TicketDto> getAllTickets() {
        if (ticketRepository.count() == 0) {
            ticketRepository.saveAll(createMockTickets());
        }

        return ticketRepository.findAll()
                .stream()
                .map(TicketMapper::toDto)
                .toList();
    }

    public TicketDto getTicketById(String id) {
        return ticketRepository.findById(id)
                .map(TicketMapper::toDto)
                .orElseThrow(() -> new NoSuchElementException("Ticket mit ID " + id + " nicht gefunden"));
    }

    public TicketDto createTicket(TicketDto newTicket) {
        Ticket ticket = new Ticket(
                null,
                null,
                newTicket.title(),
                newTicket.description(),
                newTicket.status(),
                newTicket.technician(),
                newTicket.scenarioType(),
                LocalDateTime.now(ZoneId.systemDefault())
        );

        Ticket savedTicket = ticketRepository.save(ticket);
        return TicketMapper.toDto(savedTicket);
    }

    public TicketDto updateTicket(String id, TicketDto updatedTicket) {
        if (!ticketRepository.existsById(id)) {
            throw new NoSuchElementException("Ticket mit ID " + id + " nicht gefunden");
        }

        Ticket ticket = new Ticket(
                id,
                null,
                updatedTicket.title(),
                updatedTicket.description(),
                updatedTicket.status(),
                updatedTicket.technician(),
                updatedTicket.scenarioType(),
                updatedTicket.createdAt()
        );

        Ticket savedTicket = ticketRepository.save(ticket);
        return TicketMapper.toDto(savedTicket);
    }

    public List<TicketDto> syncFromGlpi() {
        List<Map<String, Object>> glpiTickets = glpiClient.getAllGlpiTickets();

        List<Ticket> savedTickets = glpiTickets.stream()
                .map(this::upsertGlpiTicket)
                .toList();

        return savedTickets.stream()
                .map(TicketMapper::toDto)
                .toList();
    }

    private Ticket upsertGlpiTicket(Map<String, Object> glpiTicket) {
        Ticket newTicket = GlpiTicketMapper.toTicket(glpiTicket);

        return ticketRepository.findByGlpiTicketId(newTicket.getGlpiTicketId())
                .map(existingTicket -> {
                    newTicket.setId(existingTicket.getId());
                    return ticketRepository.save(newTicket);
                })
                .orElseGet(() -> ticketRepository.save(newTicket));
    }

    private List<Ticket> createMockTickets() {
        return List.of(
                new Ticket(null, "GLPI-1001", "Server Enterprise-01 Wartung",
                        "Geplantes Patching des vSphere-Clusters außerhalb der Betriebszeiten.",
                        TicketStatus.NEW, "M. Scott", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now(ZoneId.systemDefault())),
                new Ticket(null, "GLPI-1002", "Backup-Check Enterprise-02",
                        "Wöchentliche Kontrolle der Backup-Jobs.",
                        TicketStatus.IN_PROGRESS, "N. Uhura", ScenarioType.SERVER_MAINTENANCE, LocalDateTime.now(ZoneId.systemDefault()))
        );
    }
}
