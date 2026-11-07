package com.businessmanager.dto.document;

import com.businessmanager.entity.DocumentCategory;

import java.time.Instant;
import java.util.UUID;

public class DocumentDtos {

    public record DocumentResponse(
            UUID id,
            String fileName,
            String contentType,
            long size,
            DocumentCategory category,
            UUID customerId,
            String customerName,
            String uploadedBy,
            Instant createdAt
    ) {
    }
}
