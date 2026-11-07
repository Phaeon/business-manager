package com.businessmanager.dto.quote;

import com.businessmanager.dto.common.LineItemDtos.LineItemDto;
import com.businessmanager.dto.common.LineItemDtos.LineItemRequest;
import com.businessmanager.entity.QuoteStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class QuoteDtos {

    public record QuoteRequest(
            @NotNull(message = "Le client est obligatoire") UUID customerId,
            LocalDate issueDate,
            LocalDate validUntil,
            String notes,
            @NotEmpty(message = "Au moins une ligne est requise") @Valid List<LineItemRequest> lines
    ) {
    }

    public record QuoteResponse(
            UUID id,
            String number,
            UUID customerId,
            String customerName,
            QuoteStatus status,
            LocalDate issueDate,
            LocalDate validUntil,
            String notes,
            List<LineItemDto> lines,
            BigDecimal totalHt,
            BigDecimal totalVat,
            BigDecimal totalTtc,
            Instant createdAt
    ) {
    }
}
