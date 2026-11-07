package com.businessmanager.mapper;

import com.businessmanager.dto.common.LineItemDtos.LineItemDto;
import com.businessmanager.dto.quote.QuoteDtos.QuoteResponse;
import com.businessmanager.entity.LineItem;
import com.businessmanager.entity.Quote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuoteMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    QuoteResponse toResponse(Quote quote);

    LineItemDto toLineDto(LineItem line);
}
