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

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getAllTasks_returnsAllTasksAsDto() {
        Task task = new Task("1", "ticket-1", "Check backup", "Check logs",
                LocalDateTime.now(), LocalDate.now(), null, TaskStatus.OPEN);
        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<TaskDto> result = taskService.getAllTasks();

        assertEquals(1, result.size());
        assertEquals("Check backup", result.get(0).topic());
    }

    @Test
    void getTasksByTicketId_returnsOnlyTasksForThisTicket() {
        Task task = new Task("1", "ticket-1", "Check backup", "Check logs",
                LocalDateTime.now(), LocalDate.now(), null, TaskStatus.OPEN);
        when(taskRepository.findByTicketId("ticket-1")).thenReturn(List.of(task));

        List<TaskDto> result = taskService.getTasksByTicketId("ticket-1");

        assertEquals(1, result.size());
        assertEquals("ticket-1", result.get(0).ticketId());
    }

    @Test
    void getTaskById_returnsTask_whenIdExists() {
        Task task = new Task("1", "ticket-1", "Check backup", "Check logs",
                LocalDateTime.now(), LocalDate.now(), null, TaskStatus.OPEN);
        when(taskRepository.findById("1")).thenReturn(Optional.of(task));

        TaskDto result = taskService.getTaskById("1");

        assertEquals("1", result.id());
    }

    @Test
    void getTaskById_throwsException_whenIdDoesNotExist() {
        when(taskRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> taskService.getTaskById("unknown"));
    }

    @Test
    void createTask_setsRecordedAtAndDefaultStatus() {
        TaskDto newTask = new TaskDto(null, "ticket-1", "Check backup", "Check logs",
                null, LocalDate.now(), null, null);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task passed = invocation.getArgument(0);
            passed.setId("1");
            return passed;
        });

        TaskDto result = taskService.createTask(newTask);

        assertEquals("1", result.id());
        assertNotNull(result.recordedAt());
        assertEquals(TaskStatus.OPEN, result.status());
        assertNull(result.doneAt());
    }

    @Test
    void updateTask_setsDoneAt_whenStatusChangesToDone() {
        Task existingTask = new Task("1", "ticket-1", "Check backup", "Check logs",
                LocalDateTime.now().minusDays(1), LocalDate.now(), null, TaskStatus.IN_PROGRESS);

        TaskDto updatedTask = new TaskDto("1", "ticket-1", "Check backup", "done",
                null, LocalDate.now(), null, TaskStatus.DONE);

        when(taskRepository.findById("1")).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDto result = taskService.updateTask("1", updatedTask);

        assertEquals(TaskStatus.DONE, result.status());
        assertNotNull(result.doneAt());
    }

    @Test
    void updateTask_resetsDoneAt_whenStatusBecomesOpenAgain() {
        Task existingTask = new Task("1", "ticket-1", "Check backup", "Check logs",
                LocalDateTime.now().minusDays(1), LocalDate.now(), LocalDateTime.now(), TaskStatus.DONE);

        TaskDto updatedTask = new TaskDto("1", "ticket-1", "Check backup", "still open",
                null, LocalDate.now(), null, TaskStatus.OPEN);

        when(taskRepository.findById("1")).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDto result = taskService.updateTask("1", updatedTask);

        assertEquals(TaskStatus.OPEN, result.status());
        assertNull(result.doneAt());
    }

    @Test
    void updateTask_throwsException_whenIdDoesNotExist() {
        when(taskRepository.findById("unknown")).thenReturn(Optional.empty());
        TaskDto updatedTask = new TaskDto("unknown", "ticket-1", "Check backup", "x",
                null, LocalDate.now(), null, TaskStatus.OPEN);

        assertThrows(NoSuchElementException.class, () -> taskService.updateTask("unknown", updatedTask));
    }

    @Test
    void deleteTask_deletesTask_whenIdExists() {
        when(taskRepository.existsById("1")).thenReturn(true);

        taskService.deleteTask("1");

        verify(taskRepository).deleteById("1");
    }

    @Test
    void deleteTask_throwsException_whenIdDoesNotExist() {
        when(taskRepository.existsById("unknown")).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> taskService.deleteTask("unknown"));
        verify(taskRepository, never()).deleteById(any());
    }
}

