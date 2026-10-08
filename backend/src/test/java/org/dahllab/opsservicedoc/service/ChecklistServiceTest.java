package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.ChecklistDto;
import org.dahllab.opsservicedoc.dto.ChecklistItemDto;
import org.dahllab.opsservicedoc.model.Checklist;
import org.dahllab.opsservicedoc.model.ChecklistItem;
import org.dahllab.opsservicedoc.repository.ChecklistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit-Tests für ChecklistService mit gemocktem Repository, im selben
// Stil wie TaskServiceTest: ich teste hier nur die Service-Logik
// isoliert, ohne echte MongoDB-Anbindung.
@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {

    @Mock
    private ChecklistRepository checklistRepository;

    @InjectMocks
    private ChecklistService checklistService;

    // Prüft, dass getAllChecklists() alle gefundenen Checklisten als
    // DTOs zurückgibt.
    @Test
    void getAllChecklists_gibtAlleChecklistenZurueck() {
        ChecklistItem item = new ChecklistItem("item-1", "USV geprüft", false);
        Checklist checklist = new Checklist("checklist-1", "ticket-1", "Server-Wartung",
                List.of(item), LocalDateTime.now(), null);
        when(checklistRepository.findAll()).thenReturn(List.of(checklist));

        List<ChecklistDto> result = checklistService.getAllChecklists();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).titel()).isEqualTo("Server-Wartung");
    }

    // Prüft, dass getChecklistsByTicketId() die passende
    // Repository-Methode nutzt und deren Ergebnis mappt.
    @Test
    void getChecklistsByTicketId_gibtGefilterteListeZurueck() {
        ChecklistItem item = new ChecklistItem("item-1", "USV geprüft", false);
        Checklist checklist = new Checklist("checklist-1", "ticket-1", "Server-Wartung",
                List.of(item), LocalDateTime.now(), null);
        when(checklistRepository.findByTicketId("ticket-1")).thenReturn(List.of(checklist));

        List<ChecklistDto> result = checklistService.getChecklistsByTicketId("ticket-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ticketId()).isEqualTo("ticket-1");
    }

    // Prüft den Erfolgsfall von getChecklistById().
    @Test
    void getChecklistById_gibtChecklisteZurueck_wennIdExistiert() {
        ChecklistItem item = new ChecklistItem("item-1", "USV geprüft", false);
        Checklist checklist = new Checklist("checklist-1", "ticket-1", "Server-Wartung",
                List.of(item), LocalDateTime.now(), null);
        when(checklistRepository.findById("checklist-1")).thenReturn(Optional.of(checklist));

        ChecklistDto result = checklistService.getChecklistById("checklist-1");

        assertThat(result.titel()).isEqualTo("Server-Wartung");
    }

    // Prüft, dass eine unbekannte ID bei getChecklistById() zu einer
    // NoSuchElementException führt.
    @Test
    void getChecklistById_wirftException_wennIdNichtExistiert() {
        when(checklistRepository.findById("unbekannt")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checklistService.getChecklistById("unbekannt"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // Prüft, dass createChecklist() jedem Item ohne ID eine neue UUID
    // vergibt (das Frontend schickt beim Anlegen typischerweise noch
    // keine Item-IDs mit) und erstelltAm setzt.
    @Test
    void createChecklist_vergibtItemIds_wennNochKeineVorhanden() {
        ChecklistItemDto itemDto = new ChecklistItemDto(null, "USV geprüft", false);
        ChecklistDto checklistDto = new ChecklistDto(null, "ticket-1", "Server-Wartung",
                List.of(itemDto), null, null);
        when(checklistRepository.save(any(Checklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistDto result = checklistService.createChecklist(checklistDto);

        assertThat(result.items().get(0).id()).isNotNull();
        assertThat(result.erstelltAm()).isNotNull();
    }

    // Prüft, dass abgeschlossenAm automatisch gesetzt wird, wenn schon
    // beim Anlegen alle Items erledigt sind.
    @Test
    void createChecklist_setztAbgeschlossenAm_wennAlleItemsErledigt() {
        ChecklistItemDto itemDto = new ChecklistItemDto("item-1", "USV geprüft", true);
        ChecklistDto checklistDto = new ChecklistDto(null, "ticket-1", "Server-Wartung",
                List.of(itemDto), null, null);
        when(checklistRepository.save(any(Checklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistDto result = checklistService.createChecklist(checklistDto);

        assertThat(result.abgeschlossenAm()).isNotNull();
    }

    // Prüft, dass updateChecklist() abgeschlossenAm setzt, sobald durch
    // das Update alle Items erledigt sind.
    @Test
    void updateChecklist_setztAbgeschlossenAm_wennAlleItemsErledigt() {
        ChecklistItem bestehendesItem = new ChecklistItem("item-1", "USV geprüft", false);
        Checklist bestehendeChecklist = new Checklist("checklist-1", "ticket-1", "Server-Wartung",
                List.of(bestehendesItem), LocalDateTime.now(), null);
        when(checklistRepository.findById("checklist-1")).thenReturn(Optional.of(bestehendeChecklist));
        when(checklistRepository.save(any(Checklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistItemDto aktualisiertesItem = new ChecklistItemDto("item-1", "USV geprüft", true);
        ChecklistDto checklistDto = new ChecklistDto(null, "ticket-1", "Server-Wartung",
                List.of(aktualisiertesItem), null, null);

        ChecklistDto result = checklistService.updateChecklist("checklist-1", checklistDto);

        assertThat(result.abgeschlossenAm()).isNotNull();
    }

    // Prüft die Rücksetz-Logik: wird ein bereits abgeschlossenes Item
    // wieder auf "nicht erledigt" gesetzt, muss abgeschlossenAm wieder
    // null werden (analog zu Task).
    @Test
    void updateChecklist_setztAbgeschlossenAmZurueck_wennItemWiederOffenIst() {
        ChecklistItem erledigtesItem = new ChecklistItem("item-1", "USV geprüft", true);
        Checklist bestehendeChecklist = new Checklist("checklist-1", "ticket-1", "Server-Wartung",
                List.of(erledigtesItem), LocalDateTime.now(), LocalDateTime.now());
        when(checklistRepository.findById("checklist-1")).thenReturn(Optional.of(bestehendeChecklist));
        when(checklistRepository.save(any(Checklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistItemDto wiederOffenesItem = new ChecklistItemDto("item-1", "USV geprüft", false);
        ChecklistDto checklistDto = new ChecklistDto(null, "ticket-1", "Server-Wartung",
                List.of(wiederOffenesItem), null, null);

        ChecklistDto result = checklistService.updateChecklist("checklist-1", checklistDto);

        assertThat(result.abgeschlossenAm()).isNull();
    }

    // Prüft, dass ein Update auf eine unbekannte ID eine
    // NoSuchElementException wirft.
    @Test
    void updateChecklist_wirftException_wennIdNichtExistiert() {
        when(checklistRepository.findById("unbekannt")).thenReturn(Optional.empty());
        ChecklistItemDto itemDto = new ChecklistItemDto("item-1", "USV geprüft", false);
        ChecklistDto checklistDto = new ChecklistDto(null, "ticket-1", "Server-Wartung",
                List.of(itemDto), null, null);

        assertThatThrownBy(() -> checklistService.updateChecklist("unbekannt", checklistDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    // Prüft den Erfolgsfall von deleteChecklist().
    @Test
    void deleteChecklist_loeschtChecklist_wennIdExistiert() {
        when(checklistRepository.existsById("checklist-1")).thenReturn(true);

        checklistService.deleteChecklist("checklist-1");

        verify(checklistRepository).deleteById("checklist-1");
    }

    // Prüft, dass ein Löschversuch auf eine unbekannte ID eine
    // NoSuchElementException wirft, statt dass die Repository-Methode
    // still nichts tut.
    @Test
    void deleteChecklist_wirftException_wennIdNichtExistiert() {
        when(checklistRepository.existsById("unbekannt")).thenReturn(false);

        assertThatThrownBy(() -> checklistService.deleteChecklist("unbekannt"))
                .isInstanceOf(NoSuchElementException.class);

        verify(checklistRepository, never()).deleteById(any());
    }
}
