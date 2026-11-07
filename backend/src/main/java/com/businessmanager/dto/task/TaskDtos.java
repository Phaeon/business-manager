package com.businessmanager.dto.task;

import com.businessmanager.entity.TaskPriority;
import com.businessmanager.entity.TaskStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TaskDtos {

    public record TaskRequest(
            @NotNull(message = "Le projet est obligatoire") UUID projectId,
            @NotBlank(message = "Le titre est obligatoire") String title,
            String description,
            TaskPriority priority,
            TaskStatus status,
            @DecimalMin("0.00") BigDecimal estimateHours,
            @DecimalMin("0.00") BigDecimal spentHours,
            UUID assigneeId
    ) {
    }

    public record TaskStatusRequest(@NotNull TaskStatus status) {
    }

    public record TaskResponse(
            UUID id,
            UUID projectId,
            String projectName,
            String title,
            String description,
            TaskPriority priority,
            TaskStatus status,
            BigDecimal estimateHours,
            BigDecimal spentHours,
            UUID assigneeId,
            String assigneeName,
            Instant createdAt
    ) {
    }
}
