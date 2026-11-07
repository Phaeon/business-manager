package com.businessmanager.dto.common;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LineItemDtos {

    public record LineItemRequest(
            @NotBlank(message = "La description est obligatoire") String description,
            @NotNull @DecimalMin(value = "0.01", message = "La quantité doit être positive") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.00", message = "Le prix unitaire doit être positif") BigDecimal unitPrice,
            @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal vatRate,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discountPercent
    ) {
    }

    public record LineItemDto(
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal vatRate,
            BigDecimal discountPercent,
            BigDecimal totalHt,
            BigDecimal totalVat,
            BigDecimal totalTtc
    ) {
    }
}
