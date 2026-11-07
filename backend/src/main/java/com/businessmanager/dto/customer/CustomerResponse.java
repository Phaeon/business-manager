package com.businessmanager.dto.customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String company,
        String email,
        String phone,
        String address,
        String notes,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {
}
