package com.businessmanager.dto.project;

import com.businessmanager.entity.ProjectStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ProjectDtos {

    public record ProjectRequest(
            @NotBlank(message = "Le nom est obligatoire") String name,
            String description,
            @NotNull(message = "Le client est obligatoire") UUID customerId,
            @DecimalMin("0.00") BigDecimal budget,
            LocalDate startDate,
            LocalDate dueDate,
            ProjectStatus status
    ) {
    }

    public record ProjectResponse(
            UUID id,
            String name,
            String description,
            UUID customerId,
            String customerName,
            BigDecimal budget,
            LocalDate startDate,
            LocalDate dueDate,
            int progress,
            ProjectStatus status,
            Instant createdAt
    ) {
    }

    public record CommentRequest(@NotBlank(message = "Le commentaire est vide") String content) {
    }

    public record CommentResponse(UUID id, String authorName, String content, Instant createdAt) {
    }
}
