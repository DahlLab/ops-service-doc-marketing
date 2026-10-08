package org.dahllab.opsservicedoc.service;

import org.dahllab.opsservicedoc.dto.TaskDto;
import org.dahllab.opsservicedoc.model.Task;
import org.dahllab.opsservicedoc.model.TaskStatus;
import org.dahllab.opsservicedoc.repository.TaskRepository;
import org.dahllab.opsservicedoc.util.TaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<TaskDto> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(TaskMapper::toDto)
                .toList();
    }

    public List<TaskDto> getTasksByTicketId(String ticketId) {
        return taskRepository.findByTicketId(ticketId)
                .stream()
                .map(TaskMapper::toDto)
                .toList();
    }

    public TaskDto getTaskById(String id) {
        return taskRepository.findById(id)
                .map(TaskMapper::toDto)
                .orElseThrow(() -> new NoSuchElementException("Task mit ID " + id + " nicht gefunden"));
    }

    public TaskDto createTask(TaskDto newTask) {
        Task task = new Task(
                null,
                newTask.ticketId(),
                newTask.topic(),
                newTask.nextSteps(),
                LocalDateTime.now(ZoneId.systemDefault()),
                newTask.dueDate(),
                null,
                newTask.status() != null ? newTask.status() : TaskStatus.OPEN
        );

        Task savedTask = taskRepository.save(task);
        return TaskMapper.toDto(savedTask);
    }

    public TaskDto updateTask(String id, TaskDto updatedTask) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Task mit ID " + id + " nicht gefunden"));

        LocalDateTime doneAt;
        if (updatedTask.status() == TaskStatus.DONE) {
            doneAt = existingTask.getDoneAt() != null
                    ? existingTask.getDoneAt()
                    : LocalDateTime.now(ZoneId.systemDefault());
        } else {
            doneAt = null;
        }

        Task task = new Task(
                id,
                updatedTask.ticketId(),
                updatedTask.topic(),
                updatedTask.nextSteps(),
                existingTask.getRecordedAt(),
                updatedTask.dueDate(),
                doneAt,
                updatedTask.status()
        );

        Task savedTask = taskRepository.save(task);
        return TaskMapper.toDto(savedTask);
    }

    public void deleteTask(String id) {
        if (!taskRepository.existsById(id)) {
            throw new NoSuchElementException("Task mit ID " + id + " nicht gefunden");
        }
        taskRepository.deleteById(id);
    }
}
