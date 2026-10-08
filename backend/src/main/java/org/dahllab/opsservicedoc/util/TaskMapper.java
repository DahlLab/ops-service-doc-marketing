package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.dto.TaskDto;
import org.dahllab.opsservicedoc.model.Task;

// Reine Mapping-Klasse zwischen Task (MongoDB-Dokumente) und TaskDto
// (nach aussen über die API), genau wie TIcketMapper enthält sie keien
// Business-Logik (Single Responsibility Principle).
public class TaskMapper {

    private TaskMapper() {
        // Instanziierung verhindern: die Klasse enthält nur statische
        // Hilfsmethoden.
    }

    public static TaskDto toDto(Task task) {
        return new TaskDto(
                task.getId(),
                task.getTicketId(),
                task.getTopic(),
                task.getNextSteps(),
                task.getRecordedAt(),
                task.getDueDate(),
                task.getDoneAt(),
                task.getStatus()
        );
    }
}
