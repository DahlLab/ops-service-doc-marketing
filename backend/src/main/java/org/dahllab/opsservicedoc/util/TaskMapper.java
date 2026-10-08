package org.dahllab.opsservicedoc.util;

import org.dahllab.opsservicedoc.dto.TaskDto;
import org.dahllab.opsservicedoc.model.Task;

public class TaskMapper {
    private TaskMapper() {
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
