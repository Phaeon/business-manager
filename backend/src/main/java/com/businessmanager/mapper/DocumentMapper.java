package com.businessmanager.mapper;

import com.businessmanager.dto.document.DocumentDtos.DocumentResponse;
import com.businessmanager.entity.StoredDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    DocumentResponse toResponse(StoredDocument document);
}
