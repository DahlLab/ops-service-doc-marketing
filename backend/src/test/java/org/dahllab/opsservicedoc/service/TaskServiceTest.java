package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.TaskDto;
import org.dahllab.opsservicedoc.model.Task;
import org.dahllab.opsservicedoc.model.TaskStatus;
import org.dahllab.opsservicedoc.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @ExtendWith (MockitoExtension.class): aktiviert Mockito für diese
// Testklasse, damit ich @Mock und @InjectMocks nutzen kann, ohne
// selbst Mocks von Hand zu initialisieren.
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    // Mock des Pepositories: ich teste hier nur TaskService isoliert,
    // ohne eine echte MongoDB-Verbindung zu brauchen.
    @Mock
    private TaskRepository taskRepository;

    // Mockito injiziert das gemockte Repository automatisch in den
    // TaskService-Konstruktor.
    @InjectMocks
    private TaskService taskService;

    // Prüfe, dass getAllTasks() alle vom Repository gelieferten Tasks
    // korrekt in TaskDto umwandelt und zurück gibt.
    @Test
    void getAllTasks_gibtAlleTasksAlsDtoZurueck() {
        // GIVEN: das Repository liefert genau einen Task zurück
        Task task = new Task("1", "ticket-1", "Backup prüfen", "Logs checken",
                LocalDateTime.now(), LocalDate.now(), null, TaskStatus.OFFEN);
        when(taskRepository.findAll()).thenReturn(List.of(task));

        // WHEN: ich rufe getAllTasks() auf
        List<TaskDto> result = taskService.getAllTasks();

        // THEN: ich erwarte genau ein DTO mit den gleichen Werten wie der Task
        assertEquals(1, result.size());
        assertEquals("Backup prüfen", result.get(0).thema());
    }

    // Prüfe, dass getTasksByTicketId() nur Tasks liefert, die zu
    // genau diesem Ticket gehören, wichtig für die Ticket-Detailansicht.
    @Test
    void getTasksByTicketId_gibtNurTasksZuDiesemTicketZurueck() {
        // GIVEN: das Repository liefert eine Task zu "ticket-1"
        Task task = new Task("1", "ticket-1", "Backup prüfen", "Logs checken",
                LocalDateTime.now(), LocalDate.now(), null, TaskStatus.OFFEN);
        when(taskRepository.findByTicketId("ticket-1")).thenReturn(List.of(task));

        // WHEN: ich frage gezielt nach Tasks zu "ticket-1"
        List<TaskDto> result = taskService.getTasksByTicketId("ticket-1");

        // THEN: die zurückgegebene Task gehört tatsächlich zu "ticket-1"
        assertEquals(1, result.size());
        assertEquals("ticket-1", result.get(0).ticketId());
    }

    // Prüfe den Erfolgsfall von getTaskById(): existiert die ID, bekomme
    // ich das passende DTO zurück.
    @Test
    void getTaskById_gibtTasks_wennIdExistiert() {
        // GIVEN:
        Task task = new Task("1", "ticket-1", "Backup prüfen", "Logs checken",
                LocalDateTime.now(), LocalDate.now(), null, TaskStatus.OFFEN);
        when(taskRepository.findById("1")).thenReturn(Optional.of(task));

        // WHEN:
        TaskDto result = taskService.getTaskById("1");

        // THEN:
        assertEquals("1", result.id());
    }

    // Prüfe den Fehlerfall von getTaskById(): existiert die ID nicht,
    // muss eine NoSuchElementException geworfen werden statt z.B. null
    // zurückzugeben, so kann der Controller sauber mit 404 reagieren.
    @Test
    void getTaskById_wirftException_wennIdNichtExistiert() {
        // GIVEN: das Repository kennt die ID "unbekannt" nicht
        when(taskRepository.findById("unbekannt")).thenReturn(Optional.empty());

        // WHEN + THEN: der Aufruf mit dieser ID muss die erwartete Exception werfen
        assertThrows(NoSuchElementException.class, () -> taskService.getTaskById("unbekannt"));
    }

    // Prüfe, dass createTask() beim Anlegen automatisch erfasstAm setzt
    // und falls kein Status mitgegeben wurde, auf OFFEN zurückfällt,
    // ohne dass der Aufrufer sich darum kümmern muss.
    @Test
    void createTask_setztErfasstAmUndStandardStatus() {
        // GIVEN: ein neuer TaskDto ohne erfasstAm und ohne Status
        TaskDto neuerTask = new TaskDto(null, "ticket-1", "Backup prüfen", "Logs checken",
                null, LocalDate.now(), null, null);
        // Ich simuliere das Speichern: das Repository gibt den übergebenen
        // Task zurück, nachdem ich ihm eine ID vergeben habe (wie es
        // MongoDB in echt auch tun würde).
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task uebergeben = invocation.getArgument(0);
            uebergeben.setId("1");
            return uebergeben;
        });

        // WHEN: ich lege die Task an
        TaskDto result = taskService.createTask(neuerTask);

        // THEN: ID ist gesetzt, erfasstAm wurde automatisch befüllt,
        // Status ist auf OFFEN gefallen, erledigtAm bleibt leer
        assertEquals("1", result.id());
        assertNotNull(result.erfasstAm());
        assertEquals(TaskStatus.OFFEN, result.status());
        assertNull(result.erledigtAm());
    }

    // Prüfe die automatische Pflege von erledigtAm: wechselt der Status
    // auf ERLEDIGT, muss erledigtAm automatisch auf "jetzt" gesetzt
    // werden, ohne dass ich das Datum manuell im Frontend eingeben muss.
    @Test
    void updateTask_setztErledigtAm_wennStatusAufErledigtWechselt() {
        // GIVEN: ein bestehender Task, der noch IN_BEARBEITUNG ist
        Task bestehenderTask = new Task("1", "ticket-1", "Backup prüfen", "Logs checken",
                LocalDateTime.now().minusDays(1), LocalDate.now(), null, TaskStatus.IN_BEARBEITUNG);
        // und ein Update, das den Status auf ERLEDIGT setzt
        TaskDto aktualisierterTask = new TaskDto("1", "ticket-1", "Backup prüfen", "erledigt",
                null, LocalDate.now(), null, TaskStatus.ERLEDIGT);

        when(taskRepository.findById("1")).thenReturn(Optional.of(bestehenderTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN: ich führe das Update aus
        TaskDto result = taskService.updateTask("1", aktualisierterTask);

        // THEN: der Status ist ERLEDIGT und erledigtAm wurde automatisch gesetzt
        assertEquals(TaskStatus.ERLEDIGT, result.status());
        assertNotNull(result.erledigtAm());
    }

    // Prüfe den Gegenfall: wechselt ein bereits erledigter Task wieder
    // auf einen anderen Status zurück (z.B. versehentlich zu früh
    // abgehakt), muss erledigtAm wieder auf null gesetzt werden,
    // sonst stünde ein Erledigungsdatum bei einem offenen Task.
    @Test
    void updateTask_setztErledigtAmZurueck_wennStatusWiederOffenWird() {
        // GIVEN: ein bereits erledigter Task mit gesetztem erledigtAm
        Task bestehenderTask = new Task("1", "ticket-1", "Backup prüfen", "Logs checken",
                LocalDateTime.now().minusDays(1), LocalDate.now(), LocalDateTime.now(), TaskStatus.ERLEDIGT);
        // und ein Update, das ihn wieder auf OFFEN zurücksetzt
        TaskDto aktualisierterTask = new TaskDto("1", "ticket-1", "Backup prüfen", "doch noch offen",
                null, LocalDate.now(), null, TaskStatus.OFFEN);

        when(taskRepository.findById("1")).thenReturn(Optional.of(bestehenderTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN: ich führe das Update aus
        TaskDto result = taskService.updateTask("1", aktualisierterTask);

        // THEN: Status ist wieder OFFEN und erledigtAm wurde zurückgesetzt
        assertEquals(TaskStatus.OFFEN, result.status());
        assertNull(result.erledigtAm());
    }

    // Prüfe den Fehlerfall von updateTask(): existiert die ID nicht,
    // darf kein Update stattfinden, sondern es muss eine
    // NoSuchElementException fliegen.
    @Test
    void updateTask_wirftException_wennIdNichtExistiert() {
        // GIVEN: das Repository kennt die ID "unbekannt" nicht
        when(taskRepository.findById("unbekannt")).thenReturn(Optional.empty());
        TaskDto aktualisierterTask = new TaskDto("unbekannt", "ticket-1", "Backup prüfen", "x",
                null, LocalDate.now(), null, TaskStatus.OFFEN);

        // WHEN + THEN: der Aufruf muss die erwartete Exception werfen
        assertThrows(NoSuchElementException.class, () -> taskService.updateTask("unbekannt", aktualisierterTask));
    }

    // Prüfe den Erfolgsfall von deleteTask(): existiert die ID, wird
    // taskRepository.deleteById() tatsächlich mit der richtigen ID
    // aufgerufen.
    @Test
    void deleteTask_loeschtTask_wennIdExistiert() {
        // GIVEN: die ID "1" existiert laut Repository
        when(taskRepository.existsById("1")).thenReturn(true);

        // WHEN: ich lösche den Task
        taskService.deleteTask("1");

        // THEN: deleteById() wurde mit genau dieser ID aufgerufen
        verify(taskRepository).deleteById("1");
    }

    // Prüfe den Fehlerfall von deleteTask(): existiert die ID nicht,
    // darf gar nicht erst gelöscht werden - stattdessen muss eine
    // NoSuchElementException fliegen.
    @Test
    void deleteTask_wirftException_wennIdNichtExistiert() {
        // GIVEN: die ID "unbekannt" existiert laut Repository nicht
        when(taskRepository.existsById("unbekannt")).thenReturn(false);

        // WHEN + THEN: der Aufruf muss die erwartete Exception werfen,
        // und deleteById() darf dabei niemals aufgerufen worden sein
        assertThrows(NoSuchElementException.class, () -> taskService.deleteTask("unbekannt"));
        verify(taskRepository, never()).deleteById(any());
    }
}


