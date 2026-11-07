package com.businessmanager.dto.invoice;

import com.businessmanager.dto.common.LineItemDtos.LineItemDto;
import com.businessmanager.dto.common.LineItemDtos.LineItemRequest;
import com.businessmanager.entity.InvoiceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class InvoiceDtos {

    public record InvoiceRequest(
            @NotNull(message = "Le client est obligatoire") UUID customerId,
            LocalDate issueDate,
            @NotNull(message = "L'échéance est obligatoire") LocalDate dueDate,
            String notes,
            @NotEmpty(message = "Au moins une ligne est requise") @Valid List<LineItemRequest> lines
    ) {
    }

    public record PaymentRequest(
            @NotNull @DecimalMin(value = "0.01", message = "Le montant doit être positif") BigDecimal amount,
            LocalDate paidAt,
            String method,
            String reference
    ) {
    }

    public record PaymentDto(
            UUID id,
            BigDecimal amount,
            LocalDate paidAt,
            String method,
            String reference
    ) {
    }

    public record InvoiceResponse(
            UUID id,
            String number,
            UUID customerId,
            String customerName,
            UUID quoteId,
            String quoteNumber,
            InvoiceStatus status,
            LocalDate issueDate,
            LocalDate dueDate,
            String notes,
            List<LineItemDto> lines,
            BigDecimal totalHt,
            BigDecimal totalVat,
            BigDecimal totalTtc,
            BigDecimal amountPaid,
            BigDecimal balance,
            List<PaymentDto> payments,
            Instant createdAt
    ) {
    }
}
