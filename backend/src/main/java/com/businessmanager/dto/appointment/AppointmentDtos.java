package com.businessmanager.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class AppointmentDtos {

    public record AppointmentRequest(
            @NotBlank(message = "Le titre est obligatoire") String title,
            @NotNull(message = "La date de début est obligatoire") LocalDateTime startAt,
            LocalDateTime endAt,
            UUID customerId,
            String location,
            String notes
    ) {
    }

    public record AppointmentResponse(
            UUID id,
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            UUID customerId,
            String customerName,
            String location,
            String notes
    ) {
    }
}
