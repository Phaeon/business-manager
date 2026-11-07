package com.businessmanager.mapper;

import com.businessmanager.dto.common.LineItemDtos.LineItemDto;
import com.businessmanager.dto.invoice.InvoiceDtos.InvoiceResponse;
import com.businessmanager.dto.invoice.InvoiceDtos.PaymentDto;
import com.businessmanager.entity.Invoice;
import com.businessmanager.entity.LineItem;
import com.businessmanager.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "quote.id", target = "quoteId")
    @Mapping(source = "quote.number", target = "quoteNumber")
    InvoiceResponse toResponse(Invoice invoice);

    LineItemDto toLineDto(LineItem line);

    PaymentDto toPaymentDto(Payment payment);
}
